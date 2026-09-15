![Sweden Connect](images/sweden-connect.png)

# Demo Mode

The fastest way to understand the configuration model is to read the `demo` Spring profile, since it
exercises every configuration layer at once. It is activated by `./run-demo.sh`, which is equivalent to:

```bash
mvn -pl oidf-services spring-boot:run -Dspring-boot.run.profiles=demo
```

This loads `oidf-services/src/main/resources/application.yml` (the defaults, always active) and layers
`application-demo.yml` on top of it. The relevant part of `application-demo.yml` looks like this:

```yaml
credential:
  bundles:
    keystore:
      demo-federation-key-store:
        location: classpath:demo/signkey.p12
        password: changeit
        type: JKS
    jks:
      demo-federation-key:
        store-reference: demo-federation-key-store
        name: "Demo Federation Key"
        key:
          alias: 1
          key-password: changeit

federation:
  keys:
    kid-algorithm: serial
    mapping:
      federation:
        - "demo-federation-key"
      hosted:
        - "demo-hosted-key"
  service:
    storage: memory
  routing:
    enabled: true
    mode: IGNORING
  resolver:
    client:
      name: demo-resolver-client
  registry:
    integration:
      enabled: false
  local-registry:
    trust-anchors: "classpath:demo/trust-anchors.json"
    resolvers: "classpath:demo/resolvers.json"
    entities: "classpath:demo/entities.json"
```

Read top to bottom, this configures:

1. **`credential.bundles`** — two keystores (`demo-federation-key-store`, `demo-hosted-key-store`) loaded
   from PKCS#12 files on the classpath, each exposing one signing key (`demo-federation-key`,
   `demo-hosted-key`). This block is **not** an openid-federation property — it is provided by the
   [`credentials-support`](https://docs.swedenconnect.se/credentials-support/) library that Spring Boot
   auto-configures. `credential.bundles.keystore.<name>` describes *where* a keystore lives
   (`location`, `password`, `type` — a standard `java.security.KeyStore` type such as `PKCS12` or `JKS`),
   and `credential.bundles.jks.<name>` describes *which key* inside that keystore to use
   (`store-reference` points back at the keystore, `key.alias` + `key.key-password` select the entry, and
   `name` is a human-readable label used in logs/JWK metadata).
2. **`federation.keys.mapping`** — takes the credential bundle names (`demo-federation-key`,
   `demo-hosted-key`) and assigns them a *usage*: keys listed under `federation` sign
   federation-protocol artifacts (entity statements, subordinate statements, trust marks), keys listed
   under `hosted` sign an entity's own hosted metadata. This is what makes `federation:demo-federation-key`
   and `hosted:demo-hosted-key` valid key references in the JSON files below (see
   [Reference Configuration](service-configuration.md#reference-configuration)).
3. **`federation.service.storage: memory`** — state (cached entity statements, resolve trees, etc.) is
   kept in-process instead of Redis. Fine for a demo/single-node setup, not for a clustered deployment
   (see [2.2 Federation Service](service-configuration.md#22-federation-service)).
4. **`federation.routing.*`** — since demo mode hosts five entities (`/ta`, `/im`, `/op`, `/rp`,
   `/resolver`) behind one Spring Boot application on one port, internal routing must be enabled so each
   request is dispatched to the right virtual entity based on its path (see
   [Routing](internals/Routing.MD) for how this dispatch works). `mode: IGNORING` is used because the
   demo only ever runs on `localhost` — it does not need to validate the `Host` header the way a
   multi-domain deployment would with `STRICT`.
5. **`federation.local-registry.*`** — instead of a live registry (`federation.registry.integration`),
   the demo loads its entities, trust anchor hierarchy and resolver straight from three JSON files on the
   classpath. This is the same local-registry format a standalone (non-managed) production deployment
   would use, just pointed at `classpath:` instead of `file:`.
6. **`federation.resolver.client.name`** — names the outbound HTTP client the service's Resolver module
   uses when it needs to fetch entity/subordinate statements over the network (see
   [2.6 Resolver HTTP Client](service-configuration.md#26-resolver-http-client)). It is required
   configuration even in the demo, although the demo's single resolver only ever resolves entities that
   are already present in its own local registry, so this client is never actually dispatched against a
   remote host.

## The three demo JSON files

These files are the actual federation data: who the entities are, how they are keyed, how they relate to
each other, and how the resolver is configured. All three live under
`oidf-services/src/main/resources/demo/` and are wired in via `federation.local-registry.entities`,
`federation.local-registry.trust-anchors` and `federation.local-registry.resolvers` respectively.

### `entities.json` — the entities themselves

One entry per entity hosted by this service instance. This is where an entity's own metadata is declared.

```json
{
  "entity-identifier": "http://localhost:8080/op",
  "virtual-entity-id": "http://localhost:8080/op",
  "jwks": "hosted:demo-hosted-key",
  "metadata": {
    "federation_entity": {
      "organization_name": "Demo OpenID Provider"
    },
    "openid_provider": {
      "issuer": "http://localhost:8080/op"
    }
  }
}
```

* `entity-identifier` / `virtual-entity-id` — the entity's federation ID. They differ when the same
  logical entity is reachable under more than one URL/host; in the demo they are always equal.
* `jwks` — a [key reference](service-configuration.md#reference-keys) into the credential bundles
  configured above. Entities that participate in the trust chain protocol as intermediaries (trust
  anchor, resolver) typically use a `federation:`-mapped key; leaf entities (OP, RP) typically use a
  `hosted:`-mapped key, since that key also signs their protocol-specific (OIDC) metadata.
* `metadata` — an object keyed by federation entity type (`federation_entity`, `openid_provider`,
  `openid_relying_party`, `oauth_client`, `oauth_authorization_server`, `oauth_resource`,
  `trust_mark_issuer`, …) whose values are published verbatim as that type's metadata in the entity's
  configuration statement. Every entity carries `federation_entity`; the trust anchor and resolver here
  additionally publish their protocol endpoints (`federation_fetch_endpoint`,
  `federation_list_endpoint`, `federation_resolve_endpoint`) since those are what makes them
  discoverable as a Trust Anchor / Resolver rather than a leaf entity.
* `trust-mark-source` (not used in the demo) — lets an entity declare Trust Marks it holds from an
  *external* issuer, i.e. one this instance does not itself issue.

### `trust-anchors.json` — the trust hierarchy

One entry per trust anchor **or intermediate**, listing its immediate subordinates. This is what the
`/subordinate_listing` and `/fetch` endpoints serve, and it is where policy and constraints are attached.

```json
{
  "entity-identifier": "http://localhost:8080/im",
  "subordinates": [
    {
      "entity-identifier": "http://localhost:8080/op",
      "jwks": "hosted:demo-hosted-key",
      "policy": {
        "id": "MyMetadataProfile",
        "policy": {
          "metadata-policy": {
            "oauth_client": {
              "organization_identifier": {
                "value": "urn:glue:iso6523:0007:2021006552"
              },
              "organization_name": {
                "value": "Myndigheten för OIDF"
              }
            }
          }
        }
      }
    },
    {
      "entity-identifier": "http://localhost:8080/rp",
      "jwks": "hosted:demo-hosted-key"
    }
  ]
}
```

This says: the Intermediate (`/im`) is itself a subordinate of the Trust Anchor (`/ta`, see the first
entry in the file) and has two subordinates of its own, `/op` and `/rp`. The `/op` subordinate statement
additionally carries a `metadata-policy` that the *Resolver* applies when resolving `/op`'s effective
metadata — see [Policy](#policy) below.

A subordinate entry supports:

| Property               | Description                                                                     | Type              |
|-------------------------|----------------------------------------------------------------------------------|-------------------|
| `entity-identifier`     | Entity ID of the subordinate                                                     | String            |
| `virtual-entity-id`     | Overrides the entity ID used inside the statement, if different                  | String            |
| `jwks`                  | Key(s) the subordinate signs its own entity configuration with                   | Object/reference  |
| `policy`                | [Metadata policy](#policy) applied to this subordinate's metadata on resolve     | Object/reference  |
| `constraints`           | [Trust chain constraints](#constraints) enforced below this subordinate          | Object            |
| `crit`                  | Critical claims the subordinate statement must assert support for               | List\<String\>    |
| `metadata-policy-crit`  | Critical policy operators used in `policy` (per §5.1.2 of the specification)     | List\<String\>    |
| `ec-location`           | Overrides where this subordinate's own entity configuration is fetched from      | String            |

#### Policy

`policy.policy.metadata-policy` follows [§4.1.2 of OpenID Federation
1.0](https://openid.net/specs/openid-federation-1_0.html#section-4.1.2): a map of
`entity-type -> claim-name -> { operator: value }`. The standard operators are:

| Operator      | Effect                                                              |
|---------------|----------------------------------------------------------------------|
| `value`       | Force the claim to exactly this value                               |
| `add`         | Add value(s) to a list-valued claim                                 |
| `default`     | Value to use if the claim is not already present                    |
| `essential`   | `true`/`false` — whether the claim must be present after merging    |
| `one_of`      | The claim's value must be one of the listed values                  |
| `subset_of`   | The claim's (list) value must be a subset of the listed values      |
| `superset_of` | The claim's (list) value must be a superset of the listed values    |

This service also registers two operators from the [Swedish OIDC Federation
profile](https://github.com/oidc-sweden/specifications/blob/main/swedish-oidc-fed-profile.md) that are
**not** part of the base specification:

| Operator     | Effect                                                                                          |
|--------------|--------------------------------------------------------------------------------------------------|
| `regexp`     | The claim's string value must match every listed regular expression                              |
| `intersects` | The claim's (list) value must share at least one element with the listed values                  |

In the example above, the policy forces `/op`'s `oauth_client.organization_identifier` and
`oauth_client.organization_name` metadata to fixed values regardless of what `/op` itself publishes —
typical for a Trust Anchor/Intermediate that wants to assert authoritative organization identity on
behalf of a subordinate. `policy.id` is a free-text label for the policy (useful for logging/debugging);
`policy.policy` is the object that is actually evaluated.

Like `jwks`, `policy` can be given as a
[reference](service-configuration.md#reference-configuration) (e.g. `policy: file:/path/to/policy.json`)
instead of inlined, which is convenient for sharing one policy across several subordinates.

#### Constraints

`constraints` follows [§4.1.4 of OpenID Federation
1.0](https://openid.net/specs/openid-federation-1_0.html#section-4.1.4) and limits what is trusted
*below* this subordinate in the chain:

```json
"constraints": {
  "max-path-length": 1,
  "allowed-entity-types": ["openid_provider", "openid_relying_party"],
  "naming": {
    "permitted": ["https://example.com/"],
    "excluded": ["https://example.com/blocked/"]
  }
}
```

| Property               | Description                                                                 | Type           |
|-------------------------|-------------------------------------------------------------------------------|----------------|
| `max-path-length`       | Maximum number of intermediates allowed below this entity                    | Long           |
| `allowed-entity-types`  | Entity types (`openid_provider`, `openid_relying_party`, …) permitted below  | List\<String\> |
| `naming.permitted`      | URI prefixes subordinate identifiers must start with                        | List\<String\> |
| `naming.excluded`       | URI prefixes subordinate identifiers must not start with                    | List\<String\> |

The demo does not set any constraints, so any entity type and any nesting depth is allowed under `/im`.

### `resolvers.json` — the resolver

```json
{
  "entity-identifier": "http://localhost:8080/resolver",
  "trust-anchor": "http://localhost:8080/ta",
  "trusted-keys": "federation:demo-federation-key",
  "resolve-response-duration": "1H"
}
```

Each entry configures one Resolver entity: which Trust Anchor it resolves chains under
(`trust-anchor`), which key(s) it trusts to validate fetched statements with (`trusted-keys`, a
[key reference](service-configuration.md#reference-keys)), and how long a `/resolve` response's `exp`
claim should be set to relative to now (`resolve-response-duration`, an ISO‑8601 duration — `"1H"` is
shorthand accepted for `PT1H`). A resolver entry's own `entity-identifier` must also exist in
`entities.json`, since that is where its own signing key and published metadata
(`federation_resolve_endpoint`, etc.) come from.

## Running against production data instead of the demo files

The only thing that changes for a non-demo deployment using the same **standalone/local-registry** mode
is *where* these three JSON files come from and *what* they contain — swap `classpath:` for `file:` (or
mount them and point at an absolute path), and enable TLS/Redis/tracing as appropriate. If instead you
want configuration to be centrally managed and hot-reloadable without restarts, use
[**managed mode**](../README.md#managed-mode) (`federation.registry.integration.enabled: true`)
against the [OpenID Federation Registry](https://github.com/swedenconnect/openid-federation-registry)
instead of `federation.local-registry.*`. See [Service Configuration](service-configuration.md) for the
full property reference used by both modes.
