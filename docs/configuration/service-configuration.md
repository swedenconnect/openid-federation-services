# Service Configuration

The openid-federation service is configured in two layers

- Spring Boot configuration where features such as TLS, management ports, session handling, Redis,
  logging levels and so on are configured. Read more about this
  at [https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html).

- OpenId-Federation configuration modules, each module can be active or inactive independently, see below

## Resolver

Properties for `openid.federation.resolver.*`

| Property | Description                                 | Type    | Default value |
|:---------|:--------------------------------------------|:--------|:--------------|
| `active` | If the given module should be active or not | Boolean | false         | 

## Trust-Anchor

Properties for `openid.federation.trust-anchor.*`

| Property | Description                                 | Type    | Default value |
|:---------|:--------------------------------------------|:--------|:--------------|
| `active` | If the given module should be active or not | Boolean | false         |

## Trust-Mark-Issuer

Properties for `openid.federation.openid.federation.trust-mark-issuer.*`

| Property                                             | Description                                                                                                                                                           | Type                | Default value |
|:-----------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------|:--------------|
| `active`                                             | If the given module should be active or not                                                                                                                           | Boolean             | false         |---------------------------------------------|-------------------------------------------------------------------------------------------|                                       
| `trust-mark-issuer.issuer-entity-id`                 | The entity ID of the trust mark issuer, identifying the issuing organization. (mandatory)                                                                             | String              |   mandatory            |
| `trust-mark-issuer.trust-mark-validity-duration`     | Duration for which the trust mark JWT is valid, represented in ISO 8601 format (e.g., `PT30M` for 30  minutes) (mandatory)                                            | String              |   mandatory            |
| `trust-mark-issuer.sign-key`                         | Key identifier used by the issuer for signing trust marks.  (mandatory)                                                                                               | Base64 encoded JWKS |   mandatory            |
| `trust-mark-issuer.trust-marks`                      | Array of trust marks issued by the trust mark issuer.   (mandatory)                                                                                                   | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].trust-mark-id`      | Unique identifier for each trust mark, typically a URL associated with the mark.   (mandatory)                                                                        | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].logo-uri`           | URI pointing to the logo image associated with the trust mark.  (optional)                                                                                            | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].delegation`         | TrustMarkDelegation JWT. See openid federation 7.2.1,  (optional)                                                                                                     | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].ref-uri`            | Reference URI for documentation or details about the trust mark.  (optional)                                                                                          | String              |               |
| `trust-mark-issuer.trust-marks[].subjects`           | List of entities (subjects) granted the trust mark, with associated metadata for each entity. (mandatory)                                                             | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].subjects[].sub`     | Subject (entity) identifier, typically a URL indicating the specific organization granted the trust mark.  (mandatory)                                                | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].subjects[].granted` | Timestamp of when the trust mark was granted to the subject, in ISO 8601 format (UTC). (optional)                                                                     | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].subjects[].expires` | Expiry date for the subject’s trust mark, in ISO 8601 format (UTC). (optional)                                                                                        | String              |   mandatory            |
| `trust-mark-issuer.trust-marks[].subjects[].revoked` | Indicates whether the trust mark for this subject has been revoked (`true`) or remains valid (`false`).        (optional)                                             | Boolean             |               |