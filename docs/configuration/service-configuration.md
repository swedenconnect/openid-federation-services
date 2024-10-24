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

| Property | Description                                 | Type    | Default value |
|:---------|:--------------------------------------------|:--------|:--------------|
| `active` | If the given module should be active or not | Boolean | false         |