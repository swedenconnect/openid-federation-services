![Sweden Connect](images/sweden-connect.png)

# Service Configuration

The openid-federation service is configured in two layers

- Spring Boot configuration where features such as TLS, management ports, session handling, Redis,
  logging levels and so on are configured. Read more about this
  at [https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html).

- OpenId-Federation service is configuration in modules (resolver, trust-anchor, trust-mark-issuers)
    - Each individual instance of a module are called submodules.
    - Submodules can be configured either via application properties (needs restart) or
      via [REST-API](https://github.com/swedenconnect/oidf-entity-registry) registry.

---

New to this service? Read [Demo Mode Configuration](service-configuration-demo.md) first — it walks
through `application-demo.yml` and the three demo JSON files (entities, trust anchors, policy,
constraints, resolver) property by property, as a worked example of everything documented below. For how
to actually run the demo, see [Getting Started (Demo Mode)](demo.md).

---

## Federation Configuration

Some properties can be configured by object or reference.

See the section about reference configuration later on.

```
federation.*
```

Each configured federation component must be referenced an **entitiy** to function correctly.

| Property                 | Description                                                                                                                                                             | Type    | Default |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| `allow-http-ec-location` | Accept `ec-location` values with the `http` scheme. OpenID Federation Entity Configuration Hosting 1.0 only allows `https` and data URLs, so only use this for local tests. | Boolean | false   |

---

## 2.1 Federation Keys

`federation.keys.*`

| Property          | Description                                                            | Type   | Default    |
|-------------------|------------------------------------------------------------------------|--------|------------|
| `kid-algorithm`   | Key ID algorithm (`default` or `serial`), see below                    | String | default    |
| `additional-keys` | List of additional public keys that can be referenced                  | Object | –          |
| `mapping`         | Mapping of private keys to determine their usecase (federation/hosted) | Object | -          |

With `default`, a key gets the key ID given in its credential configuration, or else its JWK thumbprint
(RFC 7638). With `serial`, a key that has a certificate gets the certificate's serial number, in decimal, as key
ID, and other keys get the `default` key ID.

`federation.keys.additional-keys[*]`

| Property                    | Description                           | Type   |
|-----------------------------|---------------------------------------|--------|
| `name`                      | Logical name of the key               | String |
| `base64-encoded-public-jwk` | Base64‑encoded JWK                    | String |
| `certificate`               | PEM encoded certificate or public key | String |


Keys loaded from `federation.keys.additional-keys[*]` will be available using the `public:` prefix.

`federation.keys.mapping`

| Property     | Description                                                                                                                                         | Type           | Default |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------------|---------|
| `federation` | List of federation key names to be mapped to `federation:`                                                                                          | List\<String\> | –       |
| `hosted`     | List of hosted key names to be mapped to `hosted:` **At least one key needs to be hosted if you want a default key to be loaded from the registry** | List\<String\> | –       |

---

## 2.2 Federation Service

`federation.service.*`

| Property         | Description                           | Type   | Default |
|------------------|---------------------------------------|--------|---------|
| `storage`        | Storage backend (`memory` or `redis`) | String | memory  |
| `redis.key-name` | Redis namespace / key                 | String | –       |

`federation.service.scheduling.*`

Background jobs that keep cached state fresh. Useful to disable in tests (see
`application-integration-test.yml`, which turns both triggers off so tests control reloads explicitly).

| Property                    | Description                                                              | Type     | Default |
|------------------------------|---------------------------------------------------------------------------|----------|---------|
| `registry-trigger-enabled`   | Poll the registry for state changes once a minute (managed mode only)    | Boolean  | true    |
| `resolver-trigger-enabled`   | Periodically rebuild resolver state/cache                                | Boolean  | true    |
| `resolver-reload-rate`       | Interval between resolver state rebuilds (ISO‑8601 duration)             | Duration | PT10M   |

---

## 2.3 Routing

`federation.routing.*`

| Property         | Description                                                | Type           | Default |
|------------------|-------------------------------------------------------------|----------------|---------|
| `enabled`        | Enable internal routing support                             | Boolean        | false   |
| `mode`           | Routing mode — see below                                    | String         | –       |
| `allowed-domains` | Domains accepted when `mode` is `STRICT`                    | List\<URI\>    | –       |

`mode` must be one of:

| Value     | Behavior                                                                                     |
|-----------|-----------------------------------------------------------------------------------------------|
| `STRICT`  | Enforces the host portion of the route; required for hosting entities across multiple domains behind one instance. `allowed-domains` is mandatory in this mode. |
| `RELAXED` | Accepts any host, but does not validate it.                                                  |
| `IGNORING`| Ignores the host portion of the route entirely (used by the demo, which only ever runs on `localhost`). |

Routing dispatches an incoming request path to the correct virtual entity — see
[Routing](internals/Routing.MD) for the resolution algorithm.

---

## 2.4 Registry Integration

`federation.registry.integration.*`

| Property          | Description                         | Type         | Default |
|-------------------|-------------------------------------|--------------|---------|
| `enabled`         | Enable registry integration         | Boolean      | false   |
| `instance-id`     | Instance identifier for node groups | UUID         | –       |
| `validation-keys` | Keys used to validate registry JWTs | List<String> | Empty   |

`federation.registry.integration.client.*`

| Property                  | Description                     | Type   |
|---------------------------|---------------------------------|--------|
| `base-uri`                | Base URI of the registry        | String |
| `trust-store-bundle-name` | Trust store bundle used for TLS | String |
| `name`                    | Logical name of the client      | String |

Every key named by a key reference from the registry, such as `hosted:sign-key`, must exist on this instance. An
entity with a reference to a missing key is left out and logged as an error. A reference to a missing key in the
module records (Trust Anchors, resolvers and Trust Mark Issuers) makes the whole module load fail, and the previously
loaded modules stay in use. An entity without `jwks`, or with an empty key reference, is signed with the default
key, which is the first hosted key.

---

## 2.5 Local Registry (Federation components)

```
federation.local-registry.*
```

### 2.5.1 Resolvers

`federation.local-registry.resolvers[*]`

| Property                    | Description                              | Type     |
|-----------------------------|------------------------------------------|----------|
| `entity-identifier`         | Entity ID of the resolver                | String   |
| `trusted-keys`              | Keys used to validate fetched statements | List     |
| `trust-anchor`              | Trust anchor entity ID                   | String   |
| `resolve-response-duration` | Validity of issued resolve responses, default 7 days. In JSON files and registry records the value is an ISO-8601 duration, for example `PT1H` | Duration |

---

### 2.5.2 Trust Anchors

`federation.local-registry.trust-anchors[*]`

| Property             | Description                                  | Type   |
|----------------------|----------------------------------------------|--------|
| `entity-identifier`  | Entity ID of the trust anchor                | String |
| `subordinates`       | Subordinate entities and constraints         | List   |
| `trust-mark-issuers` | Mapping of trust mark IDs to allowed issuers | Map    |
| `trust-mark-owners`  | Trust mark ownership configuration           | List   |

See [Demo Mode Configuration → `trust-anchors.json`](service-configuration-demo.md#trust-anchorsjson--the-trust-hierarchy)
for the full list of properties a subordinate entry supports, including worked examples of `policy` and
`constraints`.

A subordinate gets an `ec_location` claim in its Subordinate Statement when `ec-location` is set, or when its
`virtual-entity-id` differs from its `entity-identifier` (the well-known location under the virtual entity ID is
then used). See [`ec-location` values](#ec-location-values) for the accepted values. A warning is logged at
startup when `ec_location` is issued for an Intermediate Entity, since OpenID Federation Entity Configuration
Hosting 1.0 recommends it for Leaf Entities only, and when `ec_location` is listed in `crit`, since entities
without support for the claim will then reject the statement.

---

### 2.5.3 Trust Mark Issuers

`federation.local-registry.trust-mark-issuers[*]`

| Property                       | Description                               | Type     | Default |
|--------------------------------|-------------------------------------------|----------|---------|
| `entity-identifier`            | Entity ID of the trust mark issuer        | String   | –       |
| `trust-mark-validity-duration` | Validity of issued trust marks (ISO‑8601) | Duration | PT30M   |
| `trust-marks`                  | List of trust marks                       | List     | –       |

`federation.local-registry.trust-mark-issuers[*].trust-marks[*]`

| Property              | Description                         | Type   |
|-----------------------|-------------------------------------|--------|
| `trust-mark-type`     | Unique identifier of the trust mark | String |
| `logo-uri`            | Logo URI                            | String |
| `ref-uri`             | Reference URI                       | String |
| `delegation`          | TrustMarkDelegation JWT             | String |
| `trust-mark-subjects` | Subjects granted this trust mark    | List   |

`federation.local-registry.trust-mark-issuers[*].trust-mark-subjects[*]`

| Property  | Description                 | Type    |
|-----------|-----------------------------|---------|
| `sub`     | Subject entity ID           | String  |
| `granted` | Grant time (UTC, ISO‑8601)  | Instant |
| `expires` | Expiry time (UTC, ISO‑8601) | Instant |
| `revoked` | Revocation flag             | Boolean |

---

### 2.5.4 Entities

`federation.local-registry.entities[*]`

| Property            | Description                             | Type                |
|---------------------|-----------------------------------------|---------------------|
| `entity-identifier` | Entity ID                               | String              |
| `jwks`              | Key reference                           | Object or reference |
| `metadata`          | Federation / OIDC metadata              | Object or reference |
| `trust-mark-source` | External trust mark references          | List                |
| `ec-location`       | Overrides entity configuration location | String              |

Entities define federation metadata, trust mark sources, and may represent trust anchors, resolvers, OPs, RPs, or trust mark issuers.

#### `ec-location` values

The same rules apply to `ec-location` of entities and of Trust Anchor subordinates, whether they come from local
configuration or from the registry:

- An `https` URL, for example `https://hosting.example.com/leaf/ec`.
- A data URL holding the Entity Configuration, `data:application/entity-statement+jwt,<jwt>`. The `;base64` form
  is not accepted.
- A path starting with `/`, which is resolved against the virtual entity ID, or the entity identifier if there is
  none. The resolved URL must follow the rules above.
- An `http` URL, only if `federation.allow-http-ec-location` is `true`.

Other values make startup fail for local configuration. Entities and subordinates from the registry with other
values are left out and logged as errors. The resolver also leaves out subordinates whose Subordinate Statement has
an `ec_location` that does not follow these rules.

---

## 2.6 Resolver HTTP Client

`federation.resolver.client.*`

The REST client the service's Resolver module uses to fetch entity configurations and subordinate
statements from other federation participants over HTTPS while walking a trust chain. Same shape as
[`federation.registry.integration.client`](#24-registry-integration):

| Property                  | Description                                                       | Type   |
|----------------------------|---------------------------------------------------------------------|--------|
| `name`                    | Logical name of the client                                         | String |
| `trust-store-bundle-name` | Trust store bundle used to validate TLS server certs               | String |
| `base-uri`                | Not used for this client — startup fails validation if this is set | String |

---

## Reference Configuration

Some properties can be configured by reference, this means that we can optionally substitute the object structure with something else. E.g. A file, or
another property loaded elsewhere (for keys).

E.g.

```yaml
federation:
  local-registry:
    entities:
      - entity-identifier: https://myidentifier1.test
        policy:
          id: my-id
          policy: ... policy object ....
      - entity-identifier: https://myidentifier2.test
        policy: file:/path/to/policy
```

### Reference formats supported

| Format      | Type                                                 |
|-------------|------------------------------------------------------|
| classpath:  | Load file from classpath                             |
| file:       | Load file from system                                |
| public:     | Load key from public keys (JWK,JWKS only)            |
| federation: | Load key from federation mapped keys (JWK,JWKS only) |
| hosted:     | Load key from hosted mapped keys (JWK,JWKS only)     |


#### Reference Keys
Reference keys can be referenced by their prefix + kid or name

E.g.
> "public:359433581122628090150675142465804663870388233428" Loads the public key for kid 359433581122628090150675142465804663870388233428
> 
> "federation:sign-key-1" Loads the keypair that is mapped for federation use.

---

## Management, Health and Observability

These are plain Spring Boot Actuator settings, not openid-federation-specific properties, but a
developer starting the service up needs them to know where to look:

| Property                        | Default (`application.yml`) | Purpose                                                        |
|----------------------------------|------------------------------|------------------------------------------------------------------|
| `server.port`                    | 8000 (8080 in `demo`)       | The federation endpoints themselves (`/ta`, `/resolver`, …)     |
| `management.server.port`         | 8081                        | A **separate** port for actuator endpoints, kept off the public traffic port |
| `management.endpoints.web.exposure.include` | `*`               | All actuator endpoints are exposed on the management port       |

Notable actuator endpoints:

* `GET http://localhost:8081/actuator/ready` — a custom endpoint (not a standard Spring Boot one) that
  reports whether the service is ready for traffic. In [managed mode](#24-registry-integration),
  configuration is fetched from the registry *after* startup, so the service will not be ready
  immediately — orchestration tooling (Kubernetes readiness probes, load balancers) should poll this
  instead of assuming readiness at process start. It returns non-200 while any
  `ReadyStateComponent` reports not-ready.
* `GET http://localhost:8081/actuator/health` — standard Spring Boot liveness/health check.
* `GET http://localhost:8081/actuator/prometheus` — Prometheus-formatted metrics (enabled by default via
  `management.prometheus.metrics.export.enabled: true`), tagged with `application_name` /
  `application_version`.
* Tracing is exported over OTLP to `management.opentelemetry.tracing.export.otlp.endpoint`, which
  defaults to `${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}` — point that environment
  variable at your collector, or override `management.tracing.sampling.probability` (default `1.0`, i.e.
  trace everything) to reduce volume. The `demo` profile disables tracing and OTLP export entirely, since
  there is normally no collector running locally.

Since the demo runs both ports on `localhost`, actuator endpoints are reachable at
`http://localhost:8081/actuator/*` while the federation service itself answers on
`http://localhost:8080/*` (see the [example requests in the README](../README.md#example-requests)).