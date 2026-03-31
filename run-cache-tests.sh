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

results.sort(key=lambda x: x[0] / x[2], reverse=True)

max_name = max(len(r[1]) for r in results) if results else 20
print(f"{'Test':<{max_name}}  {'Requests':>8}  {'Total (s)':>10}  {'Per req (ms)':>13}")
print("-" * (max_name + 37))
for time, name, count in results:
  per_req_ms = (time / count) * 1000
  print(f"{name:<{max_name}}  {count:>8}  {time:>10.3f}  {per_req_ms:>12.3f}")

total_time = sum(t for t, _, _ in results)
print("-" * (max_name + 37))
print(f"{'Total':<{max_name}}  {'':>8}  {total_time:>10.3f}")
EOF
