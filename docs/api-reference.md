![Sweden Connect](images/sweden-connect.png)

# API Reference

The service is split into multiple modules which is a group of OpenID Federation endpoints.
Each module _has_ to belong to a given entity that is present in properties or registry.

## TrustAnchor / Intermediate

### Subordinate Listing

`GET /subordinate_listing`

*Query Parameters*

| Required | Name | Description |
| :--- | :--- | :--- |
| No | `entity_type` | Filters the response on entity type. |
| No | `trust_marked` | Filters the response with entities that hold at least one Trust Mark from this entity. |
| No | `trust_mark_type` | Filters the response with entities that hold a Trust Mark of this type from this entity. |
| No | `intermediate` | Filters the response to only contain intermediates. |

`trust_marked` and `trust_mark_type` are only supported by an entity that is also a Trust Mark Issuer. Otherwise
`trust_marked=true` and `trust_mark_type` give 400 `unsupported_parameter`, and `trust_marked=false` is ignored. A
`trust_mark_type` that the entity does not issue gives 400 `invalid_request`.

A Trust Mark Issuer that is not a Trust Anchor or Intermediate Entity always returns an empty list.

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
