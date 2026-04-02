# Cache

## Overview

Caching operates on distinct layers. When a computation starts, the service commits to a specific layer version so it does not mix old and new information mid-computation.

## Snapshot Layer

When building a resolve tree, the service:

1. Creates a new layer version set to the current epoch time — this is the **snapshot layer**.
2. Populates the snapshot layer with computed data.
3. Fires an event when the computation is complete, signaling that a new snapshot version is ready.

## Scheduled Recomputation

The resolve tree is recomputed on a fixed schedule (default: every 60 minutes), controlled by:

- `federation.service.scheduling.resolver-reload-rate` — reload interval (ISO-8601 duration, default `PT60M`)
- `federation.service.scheduling.resolver-trigger-enabled` — set to `false` to disable the scheduled trigger

Each reload produces a new snapshot version, replacing the previous one.

A reload can also be triggered by a registry update notification sent to `POST /registry/notify`. The notification must be a signed JWT verifiable by the registry. This sets a refresh flag that is picked up on the next registry cron tick (every 1 minute), which then triggers a resolver reload if the registry state has changed.

## Request Handling

When a request arrives, the service determines the newest snapshot version and uses that version's local cache for the endpoint.

This ensures that cached responses from an older snapshot are never served in response to a newer snapshot's requests.
