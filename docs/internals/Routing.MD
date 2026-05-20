# Routing

This service can host multiple virtual OpenID Federation entities. This document explains how incoming requests are routed to the correct entity.

## Dynamic Endpoint Resolution

Routing happens in three steps:

### Step 1: Extract the entity base URL

The service strips the last path segment from the request URL to derive a candidate entity base URL.

**Example:**

```
https://dev.swedenconnect.se/oidf/myentity/subordinate_listing
                                           ^^^^^^^^^^^^^^^^^^^^
                                           stripped
→ https://dev.swedenconnect.se/oidf/myentity
```

**Edge case: `/.well-known/openid-federation`**

The OpenID Federation entity configuration endpoint follows the path pattern `/.well-known/openid-federation`, which contains two segments. Stripping only the last segment would leave `/.well-known` as part of the base URL, which would not match any entity.

To handle this, the service detects this suffix explicitly and strips the entire `/.well-known/openid-federation` path at once:

```
https://dev.swedenconnect.se/oidf/myentity/.well-known/openid-federation
                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                                           stripped (both segments)
→ https://dev.swedenconnect.se/oidf/myentity
```

This produces the same base URL as any other endpoint under the same entity, so the rest of the routing logic proceeds identically.

### Step 2: Look up which entity owns that URL

The service maintains an index mapping **virtual entity IDs** (the public-facing URLs) to **entity IDs** (the canonical identifiers used internally).

| Virtual Entity ID (public URL)             | Entity ID (canonical)            |
|--------------------------------------------|----------------------------------|
| `https://dev.swedenconnect.se/oidf/myentity` | `https://testdomain.test/myentity` |

The base URL from Step 1 is matched against the Virtual Entity ID column to identify which entity should handle the request.

### Step 3: Match the request to an endpoint

Once the entity is identified, the service inspects that entity's configuration to find which endpoint the full request URL corresponds to.

```json
{
  "entity_id": "https://testdomain.test/myentity",
  "federation_entity": {
    "federation_list_endpoint": "https://dev.swedenconnect.se/oidf/myentity/subordinate_listing"
  }
}
```

The original request URL (`/subordinate_listing`) matches `federation_list_endpoint`, so the request is forwarded to the Trust Anchor router module, which handles that endpoint.

## Time Complexity

The cost of resolving a request depends on which backend is used for the entity index.

**Step 1** (URL stripping) is constant-time string manipulation and contributes no asymptotic cost.

**Step 2** (entity lookup) is the dominant factor:

- With the **Redis-backed index**, lookup is O(1) — two hash lookups (`HGET`) resolve the virtual entity ID to an entity record.
- With the **in-memory index** (e.g. `CachedRecordSource` or `LocalRecordSource`), lookup is O(n) — a linear scan over all registered entity records.

If multiple record sources are configured via `CompositeRecordSource`, they are tried in priority order and short-circuit on the first match, giving O(k × n) in the worst case where k is the number of sources.

**Step 3** (endpoint matching) is O(m) where m is the number of endpoints declared by the entity, typically 2–5.

| Backend      | Overall complexity |
|--------------|--------------------|
| Redis        | O(1) + O(m) ≈ O(1) |
| In-memory    | O(n) + O(m) ≈ O(n) |

## Entity ID and Virtual Entity ID — Registry Contract

This section is aimed at developers of the **registry service**, which is the source of truth for entity records.

### The two identity fields

Each `EntityRecord` returned by the registry carries two identity fields:

| Field | JSON key | Required | Purpose |
|---|---|---|---|
| `entityIdentifier` | `entity-identifier` | Yes | The canonical entity ID — used internally and as the primary index key |
| `virtualEntityId` | `virtual-entity-id` | No | The public-facing URL — used as the routing key for incoming requests |

### When to set virtualEntityId

Set `virtualEntityId` when the public URL of an entity differs from its canonical entity ID. This is the normal case when hosting entities behind a shared domain.

**Example:** An entity whose canonical ID is `https://testdomain.test/myentity` is publicly reachable at `https://dev.swedenconnect.se/oidf/myentity`. The registry should return:

```json
{
  "entity-identifier": "https://testdomain.test/myentity",
  "virtual-entity-id": "https://dev.swedenconnect.se/oidf/myentity"
}
```

If `virtualEntityId` is omitted, the entity can only be resolved by its canonical entity ID. The routing service will still fall back to matching on `entityIdentifier` if no virtual entity ID matches the request, so omitting it is safe — but the entity will not be reachable under a virtual URL.

### How the routing service uses these fields

The service first attempts to match the request base URL against `virtualEntityId`. If that lookup fails, it falls back to matching against `entityIdentifier`. This means:

- **Virtual entity ID lookup** → routes requests coming in on the public domain
- **Entity ID fallback** → handles requests where the canonical ID is used directly

Both lookups must return the same `EntityRecord`. The registry must never return conflicting records for the same entity under different identities.

### Controlling the entity configuration endpoint with `ec-location`

By default, the routing service registers `{entityIdentifier}/.well-known/openid-federation` as the entity configuration endpoint. This is always present regardless of any other configuration.

The optional `ec-location` field on an `EntityRecord` adds an additional endpoint that the routing service will also accept requests on. This is useful when the entity configuration should be reachable at a custom path.

The value is interpreted as follows:

| Value format | Resolved endpoint |
|---|---|
| Absolute URL (`https://...`) | Used as-is |
| Path (`/some/path`) | Appended to `virtualEntityId` (or `entityIdentifier` if no virtual ID is set) |

**Example — path-relative `ec-location`:**

```json
{
  "entity-identifier": "https://testdomain.test/myentity",
  "virtual-entity-id": "https://dev.swedenconnect.se/oidf/myentity",
  "ec-location": "/entity_configuration"
}
```

This registers two valid entity configuration endpoints:

```
https://dev.swedenconnect.se/oidf/myentity/entity_configuration   ← from ec-location
https://testdomain.test/myentity/.well-known/openid-federation     ← always added
```

A request to either URL will be routed to the entity configuration handler for this entity.

Note that when `ec-location` is a relative path, it is resolved against `virtualEntityId` if present — not `entityIdentifier`. This means the custom endpoint lives on the public domain, consistent with how other endpoints are exposed.

### What the routing service indexes

The Redis-backed index builds two hash maps from the records the registry provides:

- `virtual-entity-index`: `virtualEntityId → entityIdentifier` (only populated when `virtualEntityId` is set)
- `entity-record-index`: `entityIdentifier → full EntityRecord JSON`

The index is rebuilt in full each time records are refreshed. TTL is derived from the `Expirable` wrapper the registry returns — the registry controls how long records are considered valid.
