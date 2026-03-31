#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/oidf-services/target/failsafe-reports"
SOURCE_FILE="$SCRIPT_DIR/oidf-services/src/test/java/se/swedenconnect/oidf/service/CacheTestCases.java"

echo "=== Running Redis Cache Test Suite ==="
mvn -f "$SCRIPT_DIR/pom.xml" verify \
  -pl oidf-services -am \
  -Dfailsafe.includes="**/RedisCacheTestSuite.java"

echo ""
echo "=== Test Timings ==="

REPORT_FILE="$REPORT_DIR/TEST-se.swedenconnect.oidf.service.CacheTestCases.xml"

if [ ! -f "$REPORT_FILE" ]; then
  echo "Report not found: $REPORT_FILE"
  echo "Available reports:"
  ls "$REPORT_DIR"/*.xml 2>/dev/null || echo "  (none)"
  exit 1
fi

python3 - "$REPORT_FILE" "$SOURCE_FILE" <<'EOF'
import sys
import re
import xml.etree.ElementTree as ET

report_file, source_file = sys.argv[1], sys.argv[2]

# Build method name -> display name mapping from @DisplayName annotations
display_names = {}
with open(source_file) as f:
  source = f.read()
for match in re.finditer(r'@DisplayName\("([^"]+)"\)\s+(?:void|[\w<>]+)\s+(\w+)\s*\(', source):
  display_names[match.group(2)] = match.group(1)

tree = ET.parse(report_file)
root = tree.getroot()

results = []
for testcase in root.iter('testcase'):
  raw_name = testcase.get('name', '')
  method = raw_name.split('(')[0]
  display = display_names.get(method, raw_name)
  time = float(testcase.get('time', 0))
  count_match = re.search(r'\b(\d+)\s+times\b', display)
  count = int(count_match.group(1)) if count_match else 1
  results.append((time, display, count))

results.sort(key=lambda x: x[2] / x[0] if x[0] > 0 else float('inf'))

max_name = max(len(r[1]) for r in results) if results else 20
print(f"{'Test':<{max_name}}  {'Requests':>8}  {'Total (s)':>10}  {'RPS':>10}")
print("-" * (max_name + 34))
for time, name, count in results:
  rps = count / time if time > 0 else float('inf')
  print(f"{name:<{max_name}}  {count:>8}  {time:>10.3f}  {rps:>10.1f}")

total_time = sum(t for t, _, _ in results)
print("-" * (max_name + 34))
print(f"{'Total':<{max_name}}  {'':>8}  {total_time:>10.3f}")

print()
print("=== Cache Performance Gain ===")

def base_name(name):
  return re.sub(r'\s+with(?:out)?\s+(?:no\s+)?cache', '', name, flags=re.IGNORECASE).strip()

def is_cached(name):
  n = name.lower()
  return 'with cache' in n and 'without' not in n and 'with no' not in n

cached_map = {base_name(name): count / time for time, name, count in results if time > 0 and is_cached(name)}
nocache_map = {base_name(name): count / time for time, name, count in results if time > 0 and not is_cached(name)}

pairs = sorted(set(cached_map) & set(nocache_map))
if pairs:
  max_base = max(len(p) for p in pairs)
  print(f"{'Test':<{max_base}}  {'Cached RPS':>10}  {'No-cache RPS':>12}  {'Speedup':>8}")
  print("-" * (max_base + 36))
  for name in pairs:
    c_rps = cached_map[name]
    n_rps = nocache_map[name]
    speedup = c_rps / n_rps
    print(f"{name:<{max_base}}  {c_rps:>10.1f}  {n_rps:>12.1f}  {speedup:>7.1f}x")
else:
  print("No matching cache/no-cache pairs found.")
EOF
