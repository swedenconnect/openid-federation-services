![Sweden Connect](images/sweden-connect.png)

# Getting Started (Demo Mode)

Demo mode runs a self-contained federation on `localhost:8080` with no external dependencies. It
includes a Trust Anchor, Intermediate, OpenID Provider, Relying Party, and Resolver — all
pre-configured with in-memory storage. For a property-by-property walkthrough of how it is configured,
see [Demo Mode Configuration](service-configuration-demo.md).

## Start

```bash
./run-demo.sh
```

Or directly:

```bash
mvn -pl oidf-services spring-boot:run -Dspring-boot.run.profiles=demo
```

## Demo Federation Topology

```
Trust Anchor (http://localhost:8080/ta)
└── Intermediate (http://localhost:8080/im)
    ├── OpenID Provider (http://localhost:8080/op)
    └── Relying Party (http://localhost:8080/rp)

Resolver (http://localhost:8080/resolver) → anchored at /ta
```

## Verify

Once running, verify all endpoints with:

```bash
./check-demo.sh
```

All lines should print `OK`. Exit code `0` on full pass, non-zero if any fail.

## Example Requests

**Entity Configurations**

```bash
# Trust Anchor
curl http://localhost:8080/ta/.well-known/openid-federation

# Intermediate
curl http://localhost:8080/im/.well-known/openid-federation


# OpenID Provider
curl http://localhost:8080/op/.well-known/openid-federation


# Relying Party
curl http://localhost:8080/rp/.well-known/openid-federation


# Resolver
curl http://localhost:8080/resolver/.well-known/openid-federation

```

**Subordinate Listing**

```bash
# All subordinates of Trust Anchor
curl "http://localhost:8080/ta/subordinate_listing"

# All subordinates of Intermediate
curl "http://localhost:8080/im/subordinate_listing"
```

**Fetch Subordinate Statements**

```bash
# TA's statement about Intermediate
curl "http://localhost:8080/ta/fetch?sub=http://localhost:8080/im"

# IM's statement about OP
curl "http://localhost:8080/im/fetch?sub=http://localhost:8080/op"

# IM's statement about RP (formatted as JSON)
curl -s "http://localhost:8080/im/fetch?sub=http://localhost:8080/rp" 
```

**Resolve**

```bash
# Resolve OP via Trust Anchor
curl "http://localhost:8080/resolver/resolve?sub=http://localhost:8080/op&trust_anchor=http://localhost:8080/ta"

# Resolve RP via Trust Anchor
curl "http://localhost:8080/resolver/resolve?sub=http://localhost:8080/rp&trust_anchor=http://localhost:8080/ta"

# Resolve with entity type filter
curl "http://localhost:8080/resolver/resolve?sub=http://localhost:8080/op&trust_anchor=http://localhost:8080/ta&entity_type=openid_provider"
```

**Discovery**

```bash
# Discover all entities under Trust Anchor
curl "http://localhost:8080/resolver/discovery?trust_anchor=http://localhost:8080/ta"

# Discover only OpenID Providers
curl "http://localhost:8080/resolver/discovery?trust_anchor=http://localhost:8080/ta&entity_type=openid_provider"
```
