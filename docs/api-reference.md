![Sweden Connect](images/sweden-connect.png)

# API Reference

The service is split into multiple modules which is a group of OpenID Federation endpoints.
Each module _has_ to belong to a given entity that is present in properties or registry.

## TrustAnchor / Intermediate

### Subordinate Listing

`GET /subordinate_listing`

*Query Parameters*

| Required | Name          | Description                                                            |
|----------|---------------|------------------------------------------------------------------------|
| No       | entity_type   | Filters the response on entity type                                    |
| No       | trust_marked  | Filters the response with entities that contain ATLEAST one Trust Mark |
| No       | trust_mark_type | Filters the response with entities containing a specific Trust Mark    |
| No       | intermediate  | Filters the response to only contain intermediates.                    |

A request that filters — `entity_type` or `trust_mark_type` given, or `trust_marked` or `intermediate`
set to `true` — has to know the Entity Types and trust marks of the subordinates, so it fetches the
Entity Configuration of each one over the network. A request without a filter, or with `trust_marked` or
`intermediate` set to `false`, is answered from the service's own records and makes no outbound
requests. A subordinate whose Entity Configuration cannot be fetched has unknown Entity Types and is
therefore kept in the listing, unless a trust mark filter asks for evidence it cannot give. See
[Outbound HTTP Clients](service-configuration.md#26-outbound-http-clients) for the timeouts and the
budget that bound this.

### Fetch

`GET /fetch`

*Query Parameters*

| Required | Name | Description                                                        |
|----------|------|--------------------------------------------------------------------|
| Yes      | sub  | The subject for which the Subordinate Statement is being requested |

## TrustMark

### Trust Mark Endpoint

`GET /trust_mark`

*Query Parameters*

| Required | Name          | Description                                       |
|----------|---------------|---------------------------------------------------|
| Yes      | trust_mark_type | Trust Mark identifier                             |
| Yes      | sub           | The subject for which the Trust Mark is issued to |

### Trust Mark Status

`POST /trust_mark_status`

Request body encoded as `application/x-www-form-urlencoded`.
`GET /trust_mark_status` with the same parameter as a query parameter is still accepted for backwards
compatibility, but POST is what the specification requires.

*Parameters*

| Required | Name       | Description                       |
|----------|------------|-----------------------------------|
| Yes      | trust_mark | The Trust Mark to be validated    |

Response: `200` with content type `application/trust-mark-status-response+jwt`.

### Trust Mark Listing

`GET /trust_mark_listing`

*Query Parameters*

| Required | Name          | Description                                 |
|----------|---------------|----------------------------------------------|
| Yes      | trust_mark_type | Trust Mark identifier                       |
| No       | sub           | Filter response to only contain this entity |

## Resolver

### Resolve

`GET /resolve`

*Query Parameters*

| Required | Name         | Description                                                    |
|----------|--------------|------------------------------------------------------------------|
| Yes      | sub          | Subject to resolve                                             |
| Yes      | trust_anchor | Trust Anchor to resolve via                                    |
| No       | entity_type  | Filter response to only contain entities with this entity type |

### Discovery

`GET /discovery`

*Query Parameters*

| Required | Name          | Description                                                    |
|----------|---------------|------------------------------------------------------------------|
| Yes      | trust_anchor  | Trust Anchor to resolve via                                    |
| No       | entity_type   | Filter response to only contain entities with this entity type |
| No       | trust_mark_type | Filter response to only contain entities with this trust mark  |
