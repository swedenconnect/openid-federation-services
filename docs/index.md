![Sweden Connect](images/sweden-connect.png)

# OpenID Federation Services

A set of services that implement the APIs required to support
[OpenID Federation 1.0](https://openid.net/specs/openid-federation-1_0.html) — Trust Anchor,
Intermediate, Trust Mark Issuer, and Resolver. Can run standalone with static configuration or
in managed mode against the
[OpenID Federation Registry](https://github.com/swedenconnect/openid-federation-registry).

- [Release notes](release-notes.md)

## Documentation

### Getting Started

- [Getting Started (Demo Mode)](demo.md) — Start here. Run the self-contained demo federation and
  try out the API with example `curl` requests.

### API Reference

- [API Reference](api-reference.md) — Endpoint-by-endpoint reference for the TrustAnchor/Intermediate,
  TrustMark and Resolver modules (`subordinate_listing`, `fetch`, `trust_mark`, `resolve`, `discovery`,
  …).

### Configuration

- [Demo Mode Configuration](service-configuration-demo.md) — A worked walkthrough of the demo
  profile's `application-demo.yml` and its three JSON files (entities, trust anchors, policies,
  constraints, resolver).

- [Service Configuration](service-configuration.md) — Full property reference for
  standalone (local-registry) and managed (registry-backed) mode.

- [Using OIDF Registry for Configuration](oidf-registry.md) — Running in managed mode against the
  [OpenID Federation Registry](https://github.com/swedenconnect/openid-federation-registry),
  instance groups, and readiness at startup.

### Internals

- [Routing](internals/Routing.MD) — How incoming requests are routed to the correct virtual
  entity when the service hosts multiple OpenID Federation entities.

- [Cache](internals/Cache.MD) — The layered caching model used when building the resolve tree,
  including the snapshot layer and scheduled recomputation.

- [Building a Release Docker Image](internals/docker-release.md) — How snapshot and versioned
  Docker images are built and published to `ghcr.io` by the GitHub Actions workflows.

### Build

- [Build](build.md) — Building and verifying the project, and building Docker images for
  AMD and ARM.

---

Copyright &copy; 2024-2026, [Sweden Connect](https://swedenconnect.se). Licensed under version
2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
