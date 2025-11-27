# Service Configuration

The openid-federation service is configured in two layers

- Spring Boot configuration where features such as TLS, management ports, session handling, Redis,
  logging levels and so on are configured. Read more about this
  at [https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html).

- OpenId-Federation service is configuration in modules (resolver, trust-anchor, trust-mark-issuers)
    - Each individual instance of a module are called submodules.
    - Submodules can be configured either via application properties (needs restart) or
      via [REST-API](https://github.com/swedenconnect/oidf-entity-registry) registry.

**Important:** Each configured submodule needs to be referenced to an entity to function properly. These can either
be loaded from the registry or configured with properties.

## Configuration properties

`openid.federation.*`

| Property        | Description                                                              | Type           | Default value |
|:----------------|:-------------------------------------------------------------------------|:---------------|:--------------|
| `storage`       | Storage module to use, either memory (in memory) or redis                | String         | memory        |
| `sign`          | List of credential names used for signing jwts that the service produces | List\<String\> | Empty List    |
| `kid-algorithm` | key id algorithm, thumbprint or serial                                   | String         | thumbprint    |

### Registry configuration

`openid.federation.registry.integration.*`

| Property              | Description                                                                  | Type           | Default value |
|:----------------------|:-----------------------------------------------------------------------------|:---------------|:--------------|
| `enabled`             | Set this to true to enable registry integration                              | Boolean        | false         |
| `instance-id`         | Instance id for node group                                                   | UUID           | null          |
| `endpoints.base-path` | Base path for registry.                                                      | String         | null          |
| `validation-keys`     | List of credential names used for validating jwts that the registry produces | List\<String\> | Empty List    |

### Module and submodule configuration

`openid.federation.modules.resolvers.*`

| Property            | Description                                                                                                   | Type           | Default value |
|:--------------------|:--------------------------------------------------------------------------------------------------------------|:---------------|:--------------|
| `validation-keys`   | List of credential names used for validating entity configurations that the resolver consumes                 | List\<String\> | Empty List    |
| `entity-identifier` | Entity Identifier for this resolver, this entity needs to be referenced locally, see [Entity Configuration]() | String         | Empty List    |
| `trust-anchor`      | Entity Identifier for trust-anchor for this resolver to resolve from                                          | List\<String\> | Empty List    |

`openid.federation.modules.trust-anchors.*`

| Property            | Description                                                                                                       | Type   | Default value |
|:--------------------|:------------------------------------------------------------------------------------------------------------------|:-------|:--------------|
| `entity-identifier` | Entity Identifier for this trust-anchor, this entity needs to be referenced locally, see [Entity Configuration]() | String | Empty List    |

`openid.federation.modules.trust-mark-issuers.*`

| Property                       | Description                                                                                                    | Type     | Default value |
|:-------------------------------|:---------------------------------------------------------------------------------------------------------------|:---------|:--------------|
| `entity-identifier`            | The entity ID of the trust mark issuer, identifying the issuing organization.                                  | String   |               |
| `trust-mark-validity-duration` | Duration for which the trust mark JWT is valid, represented in ISO 8601 format (e.g., `PT30M` for 30  minutes) | Duration | PT30M         |
| `trust-marks`                  | Array of trust marks issued by the trust mark issuer.                                                          | String   |               |
| `trust-marks[].trust-mark-id`  | Unique identifier for each trust mark, typically a URL associated with the mark.                               | String   |               |
| `trust-marks[].logo-uri`       | URI pointing to the logo image associated with the trust mark.                                                 | String   | optional      |
| `trust-marks[].delegation`     | TrustMarkDelegation JWT. See openid federation 7.2.1,                                                          | String   | optional      |
| `trust-marks[].ref-uri`        | Reference URI for documentation or details about the trust mark.                                               | String   | optional      |

### Trust Mark Subjects

`openid.federation.modules.trust-mark-issuers[*].trust-marks[*].trust-mark-subjects[*]`

| `sub`     | Subject (entity) identifier, typically a URL indicating the specific entity granted the trust mark. | String | |
| `granted` | Timestamp of when the trust mark was granted to the subject, in ISO 8601 format (UTC). | Instant | |
| `expires` | Expiry date for the subject’s trust mark, in ISO 8601 format (UTC). | Instant | |
| `revoked` | Indicates whether the trust mark for this subject has been revoked (`true`) or remains valid (`false`). | Boolean | false |

### Entity configuration

`openid.federation.entities.*`

| Property        | Description                                                                                                     | Type                 | Default value |
|:----------------|:----------------------------------------------------------------------------------------------------------------|:---------------------|:--------------|
| `subject`       | The entity ID of the subject                                                                                    | String               |               |
| `issuer`        | The entity ID of the issuer                                                                                     | String               |               |
| `hosted-record` | Provides additional information for this record when it is hosted by this service node                          | HostedRecordProperty | null          |
| `public-keys`   | List of credential names used for public-keys in federation metadata, will be ignored if `hosted-record` is set | List\<String\>       | Empty List    |

`openid.federation.entities[*].hosted-record`

| Property             | Description                        | Type                            | Default value |
|:---------------------|:-----------------------------------|:--------------------------------|:--------------|
| `metadata`           | Metadata for this entity           | JsonObjectProperty              | null          |
| `trust-mark-sources` | Trust mark sources for this entity | List\<TrustMarkSourceProperty\> | null          |
| `authority-hints`    | List of authority-hints            | List\<String\>                  | null          |

`openid.federation.entities[*].hosted-record[*].trust-mark-sources`

| Property        | Description               | Type   | Default value |
|:----------------|:--------------------------|:-------|:--------------|
| `issuer`        | Issuer of this trust-mark | String | null          |
| `trust-mark-id` | Id for this trust-mark    | String | null          |

### Policy configuration

`openid.federation.policies[*].*`

| Property | Description                   | Type               | Default value |
|:---------|:------------------------------|:-------------------|:--------------|
| `id`     | Id of this policy             | String             | null          |
| `policy` | Json defintion of this policy | JsonObjectProperty | null          |

#### JsonObjectProperty

Any JsonObjectProperty property can be configured with either inline json or a resource file.

| Property   | Description     | Type     | Default value |
|:-----------|:----------------|:---------|:--------------|
| `json`     | Inline json     | String   | null          |
| `resource` | Spring Resource | Resource | null          |