![Logo](images/sweden-connect.png)

# Release Notes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 

### Version 0.11.11

**Date:** 2026-08-18

* Fixed `NullPointerException` in subordinate listing filters when an entity configuration has no `trust_marks` or no `federation_entity` metadata
* Filtered subordinate listings now skip subordinates whose entity configuration cannot be fetched instead of failing the whole listing
* Filtered subordinate listings now return an empty list instead of 404 when no subordinate matches
* Fixed `NullPointerException` in trust mark collection when the trust anchor has no `trust_mark_owners` or a chain statement has no `trust_marks`
* Fixed `NullPointerException` in key configuration when a key mapping (`federation`/`hosted`) is not configured
* Resolver `/discovery` is now served under the entity identifier for entities without a virtual entity id, and matching ignores query parameters
* Added tests for resolver routing and trust mark collection edge cases

### Version 0.11.10

**Date:** 2026-06-18

* Fixed duplicate keys in `/jwks` response — keys registered under both an alias and their key ID were previously returned twice
* Added `name` claim to `/jwks` JWT response containing the configured alias names for federation and hosted keys
* `JWKSRouter` is now active when virtual entity routing is enabled
* Added integration tests for the `/jwks` endpoint across all test suites

### Version 0.11.9

**Date:** 2026-06-16

* Faster state reload — state is now reloaded more aggressively to reduce stale data windows
* Relaxed metadata type validation to accept a wider range of conformant metadata structures
* Added uptime metrics via actuator
* Added typed chain validation error codes for more actionable error reporting
* Added a demo federation for local development and testing
* Fixed typo in README quickstart

### Version 0.11.8

**Date:** 2026-05-22

* Fixed resolver returning 500 instead of 404 when a subordinate cannot be found

### Version 0.11.6

**Date:** 2026-05-22

* Fixed tracing configuration

### Version 0.11.5

**Date:** 2026-05-21

* Fixed convergence error in resolver

### Version 0.11.4

**Date:** 2026-05-20

* Added distributed tracing support (OpenTelemetry / Spring Boot Actuator tracing)
* Added trace logs throughout the federation resolution pipeline
* Fixed typo in Grafana export

### Version 0.11.3

**Date:** 2026-04-13

* Fixed Grafana export to include unresolved nodes

### Version 0.11.2

**Date:** 2026-04-13

* Fixed Grafana export: added `nodeRadius` to exported node data

### Version 0.11.1

**Date:** 2026-04-13

* Dead nodes are now filtered out from resolve, discovery, and export responses
* Added new actuator endpoint (`/actuator/dead-nodes`) to list dead nodes in the federation

#### Performance Indicators

Measured with Redis, 1000 requests per scenario. (Redis 7.4.7 via Testcontainers, Apple M2 MacBook Air, 8-core, 24 GB RAM)

**Test Timings:**

| Test | Requests | Total (s) | RPS |
|---|---:|---:|---:|
| Resolve without cache (12.87 KB) | 1000 | 4.966 | 201.4 |
| Entity Configuration without cache (3.36 KB) | 1000 | 3.489 | 286.6 |
| Trust mark status without cache (2.58 KB) | 1000 | 1.778 | 562.4 |
| Trust mark without cache (0.30 KB) | 1000 | 1.427 | 700.8 |
| Subordinate fetch without cache (0.69 KB) | 1000 | 0.512 | 1953.1 |
| Entity Configuration with cache (3.36 KB) | 1000 | 0.204 | 4902.0 |
| Trust mark status with cache (2.58 KB) | 1000 | 0.196 | 5102.0 |
| Resolve with cache (12.87 KB) | 1000 | 0.196 | 5102.0 |
| Subordinate fetch with cache (0.69 KB) | 1000 | 0.190 | 5263.2 |
| Trust mark with cache (0.30 KB) | 1000 | 0.170 | 5882.4 |

**Cache Performance Gain:**

| Scenario | Payload | Cached RPS | No-cache RPS | Speedup | Cached Mbit/s | No-cache Mbit/s |
|---|---:|---:|---:|---:|---:|---:|
| Entity Configuration | 3.36 KB | 4902.0 | 286.6 | 17.1x | 134.927 | 7.889 |
| Resolve | 12.87 KB | 5102.0 | 201.4 | 25.3x | 537.913 | 21.231 |
| Subordinate fetch | 0.69 KB | 5263.2 | 1953.1 | 2.7x | 29.750 | 11.040 |
| Trust mark | 0.30 KB | 5882.4 | 700.8 | 8.4x | 14.456 | 1.722 |
| Trust mark status | 2.58 KB | 5102.0 | 562.4 | 9.1x | 107.833 | 11.887 |

### Version 0.11.0

**Date:** 2026-04-13

* Added virtual entity IDs — a single service instance can now expose multiple logical entity identities, each with its own routing, keys, and module configuration
* Added `ModuleResponseCache` interface with in-memory and Redis implementations to cache module responses per entity ID
* Added `RedisEntityRecordIndex` for Redis-backed entity record lookups
* Added `GrafanaExportCacheWarmer` to pre-warm the export cache on startup
* Introduced `FederationBaseRouter` and related routing infrastructure to support virtual entity ID dispatch
* Added `CompositeRecordSource` and `LocalRecordSource` to compose entity record lookups across modules

### Version 0.10.8

**Date:** 2026-04-02

* Registry is now reloaded from the registry after one hour instead of after the entity configuration's `exp` claim

#### Performance Indicators

Measured with Redis, 1000 requests per scenario. (Redis 7.4.7 via Testcontainers, Apple M2 MacBook Air, 8-core, 24 GB RAM)

**Test Timings:**

| Test | Requests | Total (s) | RPS |
|---|---:|---:|---:|
| Resolve without cache (12.87 KB) | 1000 | 5.170 | 193.4 |
| Entity Configuration without cache (3.36 KB) | 1000 | 4.257 | 234.9 |
| Trust mark status without cache (2.58 KB) | 1000 | 1.921 | 520.6 |
| Trust mark without cache (0.30 KB) | 1000 | 1.640 | 609.8 |
| Subordinate fetch without cache (0.69 KB) | 1000 | 0.638 | 1567.4 |
| Entity Configuration with cache (3.36 KB) | 1000 | 2.002 | 499.5 |
| Resolve with cache (12.87 KB) | 1000 | 0.424 | 2358.5 |
| Trust mark status with cache (2.58 KB) | 1000 | 0.318 | 3144.7 |
| Subordinate fetch with cache (0.69 KB) | 1000 | 0.317 | 3154.6 |
| Trust mark with cache (0.30 KB) | 1000 | 0.287 | 3484.3 |

**Cache Performance Gain:**

| Scenario | Payload | Cached RPS | No-cache RPS | Speedup | Cached Mbit/s | No-cache Mbit/s |
|---|---:|---:|---:|---:|---:|---:|
| Entity Configuration | 3.36 KB | 499.5 | 234.9 | 2.1x | 13.749 | 6.466 |
| Resolve | 12.87 KB | 2358.5 | 193.4 | 12.2x | 248.658 | 20.393 |
| Subordinate fetch | 0.69 KB | 3154.6 | 1567.4 | 2.0x | 17.831 | 8.860 |
| Trust mark | 0.30 KB | 3484.3 | 609.8 | 5.7x | 8.563 | 1.499 |
| Trust mark status | 2.58 KB | 3144.7 | 520.6 | 6.0x | 66.463 | 11.002 |

### Version 0.10.7

**Date:** 2026-04-02

* Registry is now reloaded from the registry after one hour instead of after the entity configuration's `exp` claim

#### Performance Indicators

Measured with Redis, 1000 requests per scenario. (Redis 7.4.7 via Testcontainers, Apple M2 MacBook Air, 8-core, 24 GB RAM)

**Test Timings:**

| Test | Requests | Total (s) | RPS |
|---|---:|---:|---:|
| Resolve without cache (12.87 KB) | 1000 | 10.100 | 99.0 |
| Entity Configuration without cache (3.36 KB) | 1000 | 9.196 | 108.7 |
| Trust mark status without cache (2.58 KB) | 1000 | 4.191 | 238.6 |
| Trust mark without cache (0.30 KB) | 1000 | 3.757 | 266.2 |
| Entity Configuration with cache (3.36 KB) | 1000 | 3.222 | 310.4 |
| Subordinate fetch without cache (0.69 KB) | 1000 | 1.324 | 755.3 |
| Trust mark status with cache (2.58 KB) | 1000 | 0.965 | 1036.3 |
| Trust mark with cache (0.30 KB) | 1000 | 0.750 | 1333.3 |
| Subordinate fetch with cache (0.69 KB) | 1000 | 0.677 | 1477.1 |
| Resolve with cache (12.87 KB) | 1000 | 0.541 | 1848.4 |
| **Total** | | **34.723** | |

**Cache Performance Gain:**

| Scenario | Payload | Cached RPS | No-cache RPS | Speedup | Cached Mbit/s | No-cache Mbit/s |
|---|---:|---:|---:|---:|---:|---:|
| Entity Configuration | 3.36 KB | 310.4 | 108.7 | 2.9x | 8.543 | 2.993 |
| Resolve | 12.87 KB | 1848.4 | 99.0 | 18.7x | 194.882 | 10.439 |
| Subordinate fetch | 0.69 KB | 1477.1 | 755.3 | 2.0x | 8.349 | 4.269 |
| Trust mark | 0.30 KB | 1333.3 | 266.2 | 5.0x | 3.277 | 0.654 |
| Trust mark status | 2.58 KB | 1036.3 | 238.6 | 4.3x | 21.902 | 5.043 |

### Version 0.10.6

**Date:** 2026-04-02

* Fixed snapshot version being set to 0 during release builds
* Fixed checkstyle issues

### Version 0.10.5

**Date:** 2026-04-02

* Fixed a bug with entity configuration caching (`CacheSnapshotVersionLookup` refactor)
* Refactored node keys: removed unintended Redis namespace prefix and de-duplicated entity-id in node keys
* Registry configuration is now reloaded periodically

### Version 0.10.4

**Date:** 2026-04-01

* Made caching TTL and resolve time configurable
* Fixed Redis namespace issue
* Fixed slow non-cached resolve responses for large federations
* Fixed slow entity-configuration cache for large federations
* Added observations (metrics) for routes
* Changed performance indicator to run on a large federation due to performance issues on larger federations 

#### Performance Indicator

Measured with Redis, 10000 parallel requests per scenario. (Federation with over 500 nodes, Apple M2 MacBook Air, 8-core, 24 GB RAM)

| Scenario | Payload | Cached RPS | No-cache RPS | Speedup | Cached Mbit/s | No-cache Mbit/s |
|---|---:|---:|---:|---:|---:|---:|
| Entity Configuration | 3.36 KB | 2645.5 | 280.3 | 9.4x | 72.82 | 7.72 |
| Resolve | 12.87 KB | 2717.4 | 209.0 | 13.0x | 286.50 | 22.04 |
| Subordinate fetch | 0.69 KB | 3076.9 | 1908.4 | 1.6x | 17.39 | 10.79 |
| Trust mark | 0.30 KB | 3125.0 | 606.4 | 5.2x | 7.68 | 1.49 |
| Trust mark status | 2.58 KB | 1712.3 | 586.5 | 2.9x | 36.19 | 12.40 |

### Version 0.10.3

**Date:** 2026-03-31

* Updated to Spring Boot 4.0.4
* Bugfix for Grafana export endpoint: added query parameter to select which trust anchor to export

### Version 0.10.2

**Date:** 2026-03-31

* Added `SubordinateFetchCache` interface with in-memory and Redis implementations; `TrustAnchorRouter` now caches `/fetch` responses and performs cache lookup before creating the `TrustAnchor` instance
* Added `TrustMarkCache` interface with in-memory and Redis implementations; `TrustMarkIssuerRouter` now caches `/trust_mark` responses and performs cache lookup before creating the `TrustMarkIssuer` instance
* Refactored `TrustMarkIssuerRouter` and `ResolverRouter` to defer property resolution and factory creation until after a cache miss, avoiding unnecessary work on cache hits
* Added a local 10-second TTL cache in `CachedRecordSource` to reduce redundant lookups against the backing cache store
* Updated copyright year to 2024-2026

#### Performance Indicator

Measured with Redis, 10000 parallel requests per scenario. (Small federation test, Apple M2 MacBook Air, 8-core, 24 GB RAM)

| Scenario | Payload | Cached RPS | No-cache RPS | Speedup | Cached Mbit/s | No-cache Mbit/s |
|---|---:|---:|---:|---:|---:|---:|
| Entity Configuration | 3.36 KB | 1633.5 | 284.6 | 5.7x | 44.96 | 7.83 |
| Resolve | 12.87 KB | 3235.2 | 595.7 | 5.4x | 341.09 | 62.81 |
| Subordinate fetch | 0.69 KB | 3944.8 | 1563.7 | 2.5x | 22.30 | 8.84 |
| Trust mark | 0.30 KB | 4557.9 | 453.8 | 10.0x | 11.20 | 1.12 |
| Trust mark status | 2.58 KB | 3647.0 | 607.9 | 6.0x | 77.08 | 12.85 |

### Version 0.10.1

**Date:** 2026-03-31

* Introduced `ResolverResponseCache` and `TrustMarkStatusCache` interfaces with in-memory and Redis implementations, replacing the previous `TrustMarkStatusStore`
* Removed `CacheStateManager`; replaced with `RegistryStateTrigger` and `ResolverStateTrigger` for cleaner state management
* Added `ScrapedEntityLookup` and `ScrapedTrustMarkInfo` to carry all available scraped data through the cache layer
* Resolver response caching is now applied to the `/resolve` endpoint via `RedisResolverResponseCache` and `InMemoryResolverResponseCache`
* Refactored `VersionedInMemoryCache` and `VersionedCacheLayer` to align with the new cache interfaces
* Added `CacheTestCases` integration tests covering both in-memory and Redis cache configurations

### Version 0.10.0

**Date:** 2026-03-28

* Implemented caching pattern for all signed endpoints
* Trust mark status is now fetched and cached for all trust marks when loading the federation
* Scraped entities now carry all available information through the entity tree

### Version 0.9.16

**Date:** 2026-03-16

* `KeyProperty.mapping` now accepts a list of strings instead of a single string, allowing a key to be registered under multiple mappings simultaneously

### Version 0.9.15

**Date:** 2026-03-16

* Bugfix: entity configuration presentation no longer fails when a trust mark cannot be fetched — the failed trust mark is now skipped (with an error log) instead of propagating the exception

### Version 0.9.14

**Date:** 2026-03-16

* Bugfix: `TrustMarkIssuer` now throws a descriptive `NotFoundException` when a requested trust mark type is not configured, instead of a generic `NoSuchElementException`

### Version 0.9.13

**Date:** 2026-03-06

* Bugfix: public keys are now serialized as-is to Redis — when a JWKS contains no private keys, it is serialized using `toString(true)` directly instead of going through the reference serializer

### Version 0.9.12

**Date:** 2026-02-20

* Added fail-fast validation at startup that verifies all configured key mappings reference keys that exist in the `KeyRegistry`. If an unknown key is referenced, startup fails with a descriptive error message.

### Version 0.9.11

**Date:** 2026-02-19

* Migrated to Java 25
* Upgraded to Spring Boot 4.0.2
* Updated Jib plugin version for Java 25 compatibility
* Updated GitHub Actions workflows to use Java 25
* Added documentation for new error messages and how the default key is loaded

### Version 0.9.10

**Date:** 2026-02-18

* Added `EntityRecordDeserializer` for proper deserialization of entity records, with JWKS kid-reference support and fallback to the default key from `KeyRegistry` when no JWKS is present
* Bugfix `TrustMarkDelegation` validation: the delegation type header mismatch now correctly throws an exception (missing `throw` keyword)
* Bugfix `TrustMarkDelegation` validation: changed validated claim from `id` to `trust_mark_type` to align with the OpenID Federation 1.0 specification
* Added `TrustMarkDelegation` validation for the `alg` field in the JWT header
* Added `TrustMarkDelegation` validation that `iat` must be before `exp` when expiration is present
* Bugfix `/trust_mark_status` endpoint now returns correct content type `application/trust-mark-status-response+jwt`
* Refactored registry GSON configuration to use `EntityRecordDeserializer` and removed redundant exclusion strategy

### Version 0.9.9

**Date:** 2026-01-30

* Bugfix serialize registry configuration to redis correctly

### Version 0.9.8

**Date:** 2026-01-30

* Bugfix serialize payload on /jwks correctly

### Version 0.9.7

**Date:** 2026-01-30

* Bugfix serialize keys correctly on /jwks endpoint

### Version 0.9.6

**Date:** 2026-01-29

* Bugfix key references for public key group in properties

### Version 0.9.5

**Date:** 2026-01-29

* Implements key group reference loading via properties + registry

### Version 0.9.4

**Date:** 2026-01-27

* Bugfix for subordinate statements not serializing properly

### Version 0.9.3

**Date:** 2026-01-27

* Change name of overrideConfigurationLocation to ec_location (ec-location for properties)

### Version 0.9.2

**Date:** 2026-01-27

* Update implementation to new draft (41 → 47)

### Version 0.9.1

**Date:** 2026-01-26

* Implement basic reference loading for properties (classpath + file)

### Version 0.9.0

**Date:** 2026-01-26

* Implements OIDF services with starter modules
* Configuration overhaul


Copyright &copy;
2025-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se).
Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).