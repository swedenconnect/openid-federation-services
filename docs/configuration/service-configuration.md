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

## Federation Configuration

Some properties can be configured by object or reference.

See the section about reference configuration later on.

```
federation.*
```

Each configured federation component must be referenced an **entitiy** to function correctly.

---

## 2.1 Federation Keys

`federation.keys.*`

| Property          | Description                                           | Type   | Default    |
|-------------------|-------------------------------------------------------|--------|------------|
| `kid-algorithm`   | Key ID algorithm (`thumbprint` or `serial`)           | String | thumbprint |
| `additional-keys` | List of additional public keys that can be referenced | List   | –          |

`federation.keys.additional-keys[*]`

| Property                    | Description                           | Type   |
|-----------------------------|---------------------------------------|--------|
| `name`                      | Logical name of the key               | String |
| `base64-encoded-public-jwk` | Base64‑encoded JWK                    | String |
| `certificate`               | PEM encoded certificate or public key | String |

---

## 2.2 Federation Service

`federation.service.*`

| Property         | Description                           | Type   | Default |
|------------------|---------------------------------------|--------|---------|
| `storage`        | Storage backend (`memory` or `redis`) | String | memory  |
| `redis.key-name` | Redis namespace / key                 | String | –       |

---

## 2.3 Routing

`federation.routing.*`

| Property  | Description                     | Type    | Default |
|-----------|---------------------------------|---------|---------|
| `enabled` | Enable internal routing support | Boolean | false   |
| `mode`    | Routing mode                    | String  | –       |

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
| `resolve-response-duration` | Cache duration for resolve responses     | Duration |

---

### 2.5.2 Trust Anchors

`federation.local-registry.trust-anchors[*]`

| Property             | Description                                  | Type   |
|----------------------|----------------------------------------------|--------|
| `entity-identifier`  | Entity ID of the trust anchor                | String |
| `subordinates`       | Subordinate entities and constraints         | List   |
| `trust-mark-issuers` | Mapping of trust mark IDs to allowed issuers | Map    |
| `trust-mark-owners`  | Trust mark ownership configuration           | List   |

Subordinates may define:

* `jwks`
* `constraints` (naming, path length, entity types, etc.)
* `policy`
* `crit` and override configuration locations

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
| `trust-mark-type`       | Unique identifier of the trust mark | String |
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

| Property                          | Description                             | Type                |
|-----------------------------------|-----------------------------------------|---------------------|
| `entity-identifier`               | Entity ID                               | String              |
| `jwks`                            | Key reference                           | Object or reference |
| `metadata`                        | Federation / OIDC metadata              | Object or reference |
| `trust-mark-source`               | External trust mark references          | List                |
| `override-configuration-location` | Overrides entity configuration location | String              |

Entities define federation metadata, trust mark sources, and may represent trust anchors, resolvers, OPs, RPs, or trust mark issuers.

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

| Format     | Type                                                  |
|------------|-------------------------------------------------------|
| classpath: | Load file from classpath                              |
| file:      | Load file from system                                 |
| alias:     | Load key from all loaded keys by name (JWK,JWKS only) |
