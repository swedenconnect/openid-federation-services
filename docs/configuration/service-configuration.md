# Service Configuration

The openid-federation service is configured in two layers

- Spring Boot configuration where features such as TLS, management ports, session handling, Redis,
  logging levels and so on are configured. Read more about this
  at [https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html).

- OpenId-Federation configuration modules, each module can be active or inactive independently, see below

## Resolver

Properties for `openid.federation.resolver.*`

| Property                        | Description                                          | Type    | Default value |
|:--------------------------------|:-----------------------------------------------------|:--------|:--------------|
| `active`                        | If the resolver module should be active or not       | Boolean | false         |
| `resolvers[]`                   | List of resolver configurations                      | List    |               |
| `resolvers[].trusted-keys`      | List of trusted key aliases                          | List    |               |
| `resolvers[].entity-identifier` | Identifier of the resolver entity                    | String  |               |
| `resolvers[].trust-anchor`      | The trust anchor entity associated with the resolver | String  |               |
| `resolvers[].sign-key-alias`    | Alias of the signing key used for the resolver       | String  |               |
| `resolvers.alias`               | Alias for the resolver                               | String  |               |


## Trust-Anchor

Properties for `openid.federation.trust-anchor.*`

| Property                                          | Description                                         | Type    | Default value |
|:--------------------------------------------------|:----------------------------------------------------|:--------|:--------------|
| `active`                                          | If the trust-anchor module should be active or not  | Boolean | false         |
| `anchors`                                         | List of trust anchor configurations                 | List    |               |
| `anchors.alias`                                   | Alias for the trust anchor                          | String  |               |
| `anchors.entity-identifier`                       | Identifier of the trust anchor entity               | String  |               |
| `anchors.subordinate-listing[]`                   | List of subordinate entities under the trust anchor | List    |               |
| `anchors.subordinate-listing[].entity-identifier` | Identifier of the subordinate entity                | String  |               |
| `anchors.subordinate-listing[].policy`            | Policy applied to the subordinate entity            | String  |               |


## Trust-Mark-Issuer

Properties for `openid.federation.openid.federation.trust-mark-issuer.*`

| Property                                              | Description                                                                                                    | Type     | Default value |
|:------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------|:---------|:--------------|
| `active`                                              | If the given module should be active or not                                                                    | Boolean  | false         |---------------------------------------------|-------------------------------------------------------------------------------------------|                                       
| `trust-mark-issuers.alias`                            | Alias for where TMI is mounted, ex tmi http://localhost/tmi/trust_mark_listing                                 | Duration | PT30M         |
| `trust-mark-issuers.entity-identifier`                | The entity ID of the trust mark issuer, identifying the issuing organization.                                  | String   |               |
| `trust-mark-issuers.trust-mark-validity-duration`     | Duration for which the trust mark JWT is valid, represented in ISO 8601 format (e.g., `PT30M` for 30  minutes) | Duration | PT30M         |
| `trust-mark-issuers.trust-marks`                      | Array of trust marks issued by the trust mark issuer.                                                          | String   |               |
| `trust-mark-issuers.trust-marks[].trust-mark-id`      | Unique identifier for each trust mark, typically a URL associated with the mark.                               | String   |               |
| `trust-mark-issuers.trust-marks[].logo-uri`           | URI pointing to the logo image associated with the trust mark.                                                 | String   | optional      |
| `trust-mark-issuers.trust-marks[].delegation`         | TrustMarkDelegation JWT. See openid federation 7.2.1,                                                          | String   | optional      |
| `trust-mark-issuers.trust-marks[].ref-uri`            | Reference URI for documentation or details about the trust mark.                                               | String   | optional      |
| `trust-mark-issuers.trust-marks[].subjects`           | List of entities (subjects) granted the trust mark, with associated metadata for each entity.                  | String   |               |
| `trust-mark-issuers.trust-marks[].subjects[].sub`     | Subject (entity) identifier, typically a URL indicating the specific organization granted the trust mark.      | String   |               |
| `trust-mark-issuers.trust-marks[].subjects[].granted` | Timestamp of when the trust mark was granted to the subject, in ISO 8601 format (UTC).                         | Instant  |               |
| `trust-mark-issuers.trust-marks[].subjects[].expires` | Expiry date for the subject’s trust mark, in ISO 8601 format (UTC).                                            | Instant  |               |
| `trust-mark-issuers.trust-marks[].subjects[].revoked` | Indicates whether the trust mark for this subject has been revoked (`true`) or remains valid (`false`).        | Boolean  | false         |

## Entity-Registry

Properties for `openid.federation.openid.federation.entity-registry.*`

| Property                            | Description                                  | Type    | Default value |
|:------------------------------------|:---------------------------------------------|:--------|:--------------|
| `active`                            | If the given module should be active or not  | Boolean | false         |
| `base-path`                         | Base path for the entity registry            | String  |               |
| `entity-registry.path`              | Path within the entity registry              | String  |               |
| `entity-registry.entity-identifier` | Identifier of the entity                     | String  |               |
| `entity-registry.is-root`           | Specifies if the entity is a root entity     | Boolean | false         |
| `entity-registry.sign-key-alias`    | Alias of the signing key used for the entity | String  |               |
| `entity-registry.organization-name` | Name of the organization tied to the entity  | String  |               |


## Key-Registry

Properties for `openid.federation.openid.federation.key-registry.*`

| Property                  | Description                      | Type   | Default value |
|:--------------------------|:---------------------------------|:-------|:--------------|
| `key-registry.keys`       | List of keys in the key registry | List   |               |
| `key-registry.keys.alias` | Alias of the key                 | String |               |
| `key-registry.keys.key`   | The actual key value             | String |               |
