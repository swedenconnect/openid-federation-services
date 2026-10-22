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

This client also takes the pool and timeout properties documented under
[Outbound HTTP Clients](#26-outbound-http-clients).

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

## 2.6 Outbound HTTP Clients

### How outbound requests are handled

The service makes outbound HTTPS requests to other federation participants in two situations, and they
behave very differently.

**In the background.** The Resolver rebuilds the federation tree every
[`resolver-reload-rate`](#22-federation-service), fetching the Entity Configuration of every entity in
the federation and the Subordinate Statements of every Intermediate. It fans out over the tree in
parallel, so one slow participant does not slow down the others, but the rebuild is not finished until
the slowest one has answered or timed out.

**While serving a request.** Two endpoints reach out to other participants before they can answer. A
`subordinate_listing` with a filter has to know the Entity Types of the subordinates, so it fetches the
Entity Configuration of each one. An Entity Configuration that is configured with a trust mark source
fetches those trust marks from their issuers. Both of these run in the request thread and make their
requests one at a time, so their cost is the sum of every request they make.

The two situations have **separate clients with separate connection pools and timeouts**. A burst of
inbound requests therefore cannot exhaust the connections the background rebuild needs, and the two can
be tuned independently: the request path needs to fail fast, while the background rebuild can afford to
wait.

Every stage of a request is bounded by its own timeout, because a request that hangs in one stage is not
bounded by the others. The sum of the stages is the worst case for a single request — see
[Timeouts](#timeouts) — and a request that takes that long is a request that failed, not a slow one.

Two things are **not** covered by the timeouts:

- **A sequence of requests.** The underlying HTTP client bounds one request, not a series of them. An
  endpoint that makes one request per subordinate is therefore only bounded by the number of
  subordinates that fail to answer. The filtered `subordinate_listing` has a
  [total budget](#listing-budget) for this reason.
- **Name resolution.** The `connect` timeout starts once the host has been resolved. A name server that
  does not answer is bounded by the resolver settings of the host, not by these properties.

Finally, responses are cached per snapshot of the federation tree, so a successful listing is normally
served from the cache and makes no outbound requests at all. The cache is replaced when the tree is
rebuilt, which means the first request after each rebuild is the one that pays the full cost.

### The clients

| Properties                                    | Used for                                                                 |
|-----------------------------------------------|--------------------------------------------------------------------------|
| `federation.request-client.*`                 | Requests made while serving an inbound request                            |
| `federation.resolver.client.*`                | Requests made by the background rebuild of the federation tree            |
| `federation.registry.integration.client.*`    | Requests to the [registry](#24-registry-integration) in managed mode      |

All three take the same properties:

| Property                  | Description                                                                             | Type    | Default |
|---------------------------|-----------------------------------------------------------------------------------------|---------|---------|
| `name`                    | Logical name of the client. Used in log messages and as the `client` tag on its metrics  | String  | `federation-request` for `request-client`, otherwise – |
| `trust-store-bundle-name` | Trust store bundle used to validate TLS server certificates, or `default` for the JVM's | String  | –       |
| `base-uri`                | Base URI. Only used by the registry client; startup fails if it is set on the others     | String  | –       |
| `max-retries`             | Times a failed request is retried. Each retry costs another worst case, so see below     | Integer | 0       |
| `retry-interval`          | Interval between retries, when `max-retries` is above zero                               | Duration| PT0.5S  |
| `follow-redirects`        | Whether redirects are followed. A redirect costs another worst case, and no federation endpoint needs one | Boolean | false |

`federation.request-client.trust-store-bundle-name` defaults to the value configured for
`federation.resolver.client`, since both talk to the same participants.

#### Pool

`<client>.pool.*`

Each client has its own connection pool. The pool is a ceiling, not a reservation — connections are
opened when they are needed.

| Property                    | Description                                                                                  | Type     | Default                                 |
|-----------------------------|----------------------------------------------------------------------------------------------|----------|-----------------------------------------|
| `max-total`                 | Connections the client may hold in total, across all hosts                                   | Integer  | 100, 50 for `request-client`            |
| `max-per-route`             | Connections the client may hold to one host                                                   | Integer  | 24, 10 for `request-client`             |
| `time-to-live`              | How long a connection may be reused before it is closed, regardless of activity               | Duration | PT5M                                    |
| `validate-after-inactivity` | How long a pooled connection may be idle before it is checked for staleness when leased       | Duration | PT5S                                    |
| `evict-idle-after`          | Idle connections are closed after this long. Zero keeps them until their time to live runs out | Duration | PT30S                                   |

`max-per-route` matters more than it looks. Several entities often share a host — a test federation
where fifteen Relying Parties live under the same domain is not unusual — and the background rebuild
fetches all of them at the same time. A `max-per-route` below the number of entities on a host makes
those requests run in waves of that size instead of in parallel.

For the request path the value should be kept low rather than high. It is the ceiling on how many
connections a burst of inbound requests can open against a single participant, and a participant that
rate-limits will answer `429` if that ceiling is generous.

#### Timeouts

`<client>.timeouts.*`

| Property             | Description                                                                                 | Type     | Default                        |
|----------------------|---------------------------------------------------------------------------------------------|----------|--------------------------------|
| `connection-request` | Waiting for a connection from the pool. Turns an exhausted pool into a fast failure         | Duration | PT5S, PT1S for `request-client`|
| `connect`            | Establishing the TCP connection. Name resolution happens first and is not covered           | Duration | PT3S, PT2S for `request-client`|
| `tls-handshake`      | Completing the TLS handshake, once the TCP connection is up                                  | Duration | PT3S, PT2S for `request-client`|
| `response`           | Waiting for the first byte of the response, once the request has been sent                   | Duration | PT5S, PT3S for `request-client`|
| `socket`             | Waiting between reads on the socket. Bounds a response that starts and then stops part way   | Duration | PT5S, PT3S for `request-client`|

The worst case for a single request is `connection-request + connect + tls-handshake + response`, which
is **8 seconds** for the request path and **16 seconds** for the background rebuild with the defaults.
The effective worst case is multiplied by `max-retries + 1`, and again by each redirect that is followed.

#### Listing budget

| Property                                                 | Description                                                                                        | Type     | Default |
|----------------------------------------------------------|----------------------------------------------------------------------------------------------------|----------|---------|
| `federation.trust-anchor.subordinate-listing-budget`     | The longest a filtered `subordinate_listing` may spend fetching Entity Configurations               | Duration | PT10S   |

A filtered `subordinate_listing` fetches the Entity Configuration of each subordinate in turn, so its
cost grows with the number of subordinates that do not answer. The budget bounds the endpoint as a
whole. When it is spent, the subordinates that have not been fetched are treated as if their Entity
Configuration could not be fetched, which keeps them in the listing — an unfetched subordinate has
unknown Entity Types and is therefore not filtered out (OpenID Federation 1.0, Section 8.2.1) — and a
warning is logged.

Set it below the timeout of whatever calls the endpoint, and above the worst case of a single request,
or a single unreachable subordinate spends the whole budget.

#### Metrics

Each client reports its pool as `oidf.http.client.pool`, tagged with `client` and with `state` set to
`leased`, `available`, `pending` or `max`. A pool that has run out shows up as `leased` at `max` with a
growing `pending`, which is otherwise only visible as requests that are mysteriously slow.

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