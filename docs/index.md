![Sweden Connect](images/sweden-connect.png)

# OpenID Federation Services

A set of services that implement the APIs required to support
[OpenID Federation 1.0](https://openid.net/specs/openid-federation-1_0.html) — Trust Anchor,
Intermediate, Trust Mark Issuer, and Resolver. Can run standalone with static configuration or
in managed mode against the
[OpenID Federation Registry](https://github.com/swedenconnect/openid-federation-registry).

- [Release notes](release-notes.md)

## Documentation

### Configuration

- [Demo Mode](demo-mode.md) — Start here. A worked walkthrough of the demo profile's
  `application-demo.yml` and its three JSON files (entities, trust anchors, policies,
  constraints, resolver).

- [Service Configuration](service-configuration.md) — Full property reference for
  standalone (local-registry) and managed (registry-backed) mode.

### Internals

- [Routing](internals/Routing.MD) — How incoming requests are routed to the correct virtual
  entity when the service hosts multiple OpenID Federation entities.

- [Cache](internals/Cache.MD) — The layered caching model used when building the resolve tree,
  including the snapshot layer and scheduled recomputation.

### Build

- [Build](build.md) — Building and verifying the project, and building Docker images for
  AMD and ARM.

---

Copyright &copy; 2024-2026, [Sweden Connect](https://swedenconnect.se). Licensed under version
2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
