![Logo](images/sweden-connect.png)

# Release Notes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 

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