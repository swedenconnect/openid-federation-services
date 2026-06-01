#!/usr/bin/env bash
set -euo pipefail

BASE="http://localhost:8080"
PASS=0
FAIL=0

run() {
  local desc="$1"
  local url="$2"
  printf "%-60s " "$desc"
  if curl -sf "$url" > /dev/null 2>&1; then
    echo "OK"
    ((++PASS))
  else
    echo "FAIL"
    ((++FAIL))
  fi
}

echo "=== Entity Configurations ==="
run "Trust Anchor"  "$BASE/ta/.well-known/openid-federation"
run "Intermediate"  "$BASE/im"/.well-known/openid-federation
run "OpenID Provider" "$BASE/op/.well-known/openid-federation"
run "Relying Party" "$BASE/rp/.well-known/openid-federation"
run "Resolver"      "$BASE/resolver/.well-known/openid-federation"

echo ""
echo "=== Subordinate Listing ==="
run "TA subordinates" "$BASE/ta/subordinate_listing"
run "IM subordinates" "$BASE/im/subordinate_listing"

echo ""
echo "=== Fetch Subordinate Statements ==="
run "TA fetch IM"  "$BASE/ta/fetch?sub=$BASE/im"
run "IM fetch OP"  "$BASE/im/fetch?sub=$BASE/op"
run "IM fetch RP"  "$BASE/im/fetch?sub=$BASE/rp"

echo ""
echo "=== Resolve ==="
run "Resolve OP"                     "$BASE/resolver/resolve?sub=$BASE/op&trust_anchor=$BASE/ta"
run "Resolve RP"                     "$BASE/resolver/resolve?sub=$BASE/rp&trust_anchor=$BASE/ta"
run "Resolve OP (entity_type filter)" "$BASE/resolver/resolve?sub=$BASE/op&trust_anchor=$BASE/ta&entity_type=openid_provider"

echo ""
echo "=== Discovery ==="
run "Discover all"              "$BASE/resolver/discovery?trust_anchor=$BASE/ta"
run "Discover openid_providers" "$BASE/resolver/discovery?trust_anchor=$BASE/ta&entity_type=openid_provider"

echo ""
echo "PASS: $PASS  FAIL: $FAIL"
[[ $FAIL -eq 0 ]]
