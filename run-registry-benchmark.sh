#!/usr/bin/env bash
#
# Registry integration benchmark: fake registry + oidf-services (own JVM) + redis (testcontainers).
#
# Usage:
#   ./run-registry-benchmark.sh [-Dbenchmark.<option>=<value> ...]
#   ./run-registry-benchmark.sh compare <before.json> <after.json>
#
# Options (defaults in parentheses):
#   benchmark.sizes                 leaf counts, comma separated               (100,1000,5000)
#   benchmark.leavesPerIntermediate leaves per intermediate                    (500)
#   benchmark.trustMarkTypes        trust mark types, every leaf is a subject  (2)
#   benchmark.extraMetadataBytes    padding per leaf metadata                  (0)
#   benchmark.resolver              configure a resolver                       (true)
#   benchmark.virtualEntityRouting  federation.routing.virtual-entity-routing  (true)
#   benchmark.heap                  -Xmx of the service                        (1g)
#   benchmark.requests              requests per scenario                      (2000)
#   benchmark.concurrency           concurrent requests                        (16)
#   benchmark.reloads               reloads under load                         (3)
#   benchmark.label                 label of the run                           (git short hash)
#   benchmark.jfr                   write a JFR recording of the service       (false)
#
# Reports are written to oidf-services/target/benchmark/ as .json and .md, service logs next to them.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ "${1:-}" == "compare" ]]; then
  python3 - "$2" "$3" <<'EOF'
import json
import sys

before, after = (json.load(open(path)) for path in sys.argv[1:3])

def runs(report):
  return {run["leaves"]: run for run in report["runs"] if "error" not in run and "reloads" in run}

def p99(run, scenario, cache="no-cache"):
  for s in run["scenarios"]:
    if s["name"] == scenario and s["cache"] == cache:
      return s["p99Ms"]
  return None

def worst(run, field):
  values = [r[field] for r in run["reloads"]["reloads"]]
  return None if not values or min(values) < 0 else max(values)

metrics = {
  "wire MB": lambda r: (r["payload"]["entityWireBytes"] + r["payload"]["moduleWireBytes"]) / 2**20,
  "ready s": lambda r: r["startup"]["readyMillis"] / 1000,
  "EC p99 ms": lambda r: p99(r, "entity-configuration"),
  "fetch p99 ms": lambda r: p99(r, "fetch"),
  "subordinate-listing p99 ms": lambda r: p99(r, "subordinate-listing"),
  "trust-mark p99 ms": lambda r: p99(r, "trust-mark"),
  "trust-mark-status p99 ms": lambda r: p99(r, "trust-mark-status"),
  "trust-mark-listing p99 ms": lambda r: p99(r, "trust-mark-listing"),
  "resolve p99 ms": lambda r: p99(r, "resolve"),
  "unmatched p99 ms": lambda r: p99(r, "unmatched"),
  "reload→registry ms": lambda r: worst(r, "registryConvergedMillis"),
  "reload→resolver ms": lambda r: worst(r, "resolverConvergedMillis"),
  "reload→cached ms": lambda r: worst(r, "cachedConvergedMillis"),
  "failed during reload": lambda r: sum(x["duringReload"]["unexpected"] for x in r["reloads"]["reloads"]),
  "peak heap MB (reload)": lambda r: max(x["peakHeapBytes"] for x in r["reloads"]["reloads"]) / 2**20,
  "retained heap MB": lambda r: r["resources"]["retainedHeapBytes"] / 2**20,
  "redis MB": lambda r: r["resources"]["redisUsedMemoryBytes"] / 2**20,
}

b, a = runs(before), runs(after)
print(f"before: {before['label']} ({before['timestamp']})")
print(f"after:  {after['label']} ({after['timestamp']})")
for leaves in sorted(set(b) & set(a)):
  print(f"\n{leaves} leaves")
  print(f"  {'metric':<28} {'before':>12} {'after':>12} {'change':>9}")
  for name, metric in metrics.items():
    x, y = metric(b[leaves]), metric(a[leaves])
    fx = "-" if x is None else f"{x:.2f}"
    fy = "-" if y is None else f"{y:.2f}"
    change = f"{(y - x) / x * 100:+.0f}%" if x and y is not None else ""
    print(f"  {name:<28} {fx:>12} {fy:>12} {change:>9}")
EOF
  exit 0
fi

LABEL="$(git -C "$SCRIPT_DIR" rev-parse --short HEAD 2>/dev/null || echo unlabeled)"
if ! git -C "$SCRIPT_DIR" diff --quiet 2>/dev/null; then
  LABEL="$LABEL-dirty"
fi

echo "=== Building oidf-services and test classpath ==="
mvn -q -f "$SCRIPT_DIR/pom.xml" -pl oidf-services -am test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/benchmark.classpath -Dmdep.includeScope=test

SERVICE_DIR="$SCRIPT_DIR/oidf-services"
CLASSPATH="$SERVICE_DIR/target/test-classes:$SERVICE_DIR/target/classes:$(cat "$SERVICE_DIR/target/benchmark.classpath")"

echo "=== Running registry benchmark ==="
cd "$SCRIPT_DIR"
# api.version: docker API version for testcontainers, same as surefire/failsafe in oidf-services/pom.xml
exec java -cp "$CLASSPATH" \
  -Dapi.version=1.44 \
  -Dbenchmark.label="$LABEL" \
  -Dbenchmark.outputDir="$SERVICE_DIR/target/benchmark" \
  "$@" \
  se.swedenconnect.oidf.service.benchmark.RegistryBenchmark
