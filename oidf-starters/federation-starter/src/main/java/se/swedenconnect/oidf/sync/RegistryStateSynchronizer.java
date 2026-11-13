/*
 * Copyright 2024-2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.sync;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.RestClientRecordIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.StateChanges;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Keeps the records of one instance in sync with the registry.
 *
 * <p>A sync pages through the changes since the applied sequence number and applies them in sequence order to a
 * pending copy of the state. When the first page is marked full, the pending copy starts empty and the pages from
 * sequence number 0 replace the state. When a page is marked full in a sync that did not start from sequence number
 * 0, the sync restarts from sequence number 0. The pending copy replaces the active state in the
 * {@link RegistrySyncStateStore} only when the sync succeeds.</p>
 *
 * @author Felix Hellman
 */
@Slf4j
public class RegistryStateSynchronizer {

  private final UUID instanceId;
  private final RestClientRecordIntegration client;
  private final RegistrySyncStateStore store;

  /**
   * Constructor.
   *
   * @param instanceId of the instance to sync
   * @param client     for fetching from the registry
   * @param store      for the active state
   */
  public RegistryStateSynchronizer(
      final UUID instanceId,
      final RestClientRecordIntegration client,
      final RegistrySyncStateStore store) {

    this.instanceId = instanceId;
    this.client = client;
    this.store = store;
  }

  /**
   * Syncs the state with the registry. A failure leaves the active state untouched.
   *
   * @return the synced state, expiring at the earliest expiry of the pages applied
   */
  public synchronized Expirable<RegistrySyncState> sync() {
    final Optional<RegistrySyncState> active = this.store.load(this.instanceId);
    final RegistrySyncState empty = new RegistrySyncState(0, new TreeMap<>());
    Result result = this.run(active.orElse(empty));
    if (result == null) {
      log.info("Registry requires a full sync of instance {}, restarting from seq 0", this.instanceId);
      result = this.run(empty);
      if (result == null) {
        throw new IllegalStateException("Registry did not mark the changes from seq 0 of instance %s as full"
            .formatted(this.instanceId));
      }
    }
    if (result.changed()) {
      this.store.store(this.instanceId, result.state());
    }
    log.debug("Synced instance {} to seq {} with {} records", this.instanceId, result.state().seq(),
        result.state().records().size());
    return new Expirable<>(result.validity().expiration, result.validity().issuedAt, result.state());
  }

  /**
   * Asks the registry for the changes since the applied sequence number, without applying them.
   *
   * @return true if there is no synced state, or the registry has changes since it or requires a full sync
   */
  public boolean hasChanges() {
    final Optional<Long> seq = this.store.seq(this.instanceId);
    if (seq.isEmpty()) {
      return true;
    }
    final StateChanges changes = this.client.getChanges(this.instanceId, seq.get()).getValue();
    return changes.full() || !Optional.ofNullable(changes.changes()).orElseGet(List::of).isEmpty();
  }

  /**
   * Runs one sync from the seq of the base state.
   *
   * @param base state to apply the changes to
   * @return result, or null if the registry requires a restart from seq 0
   */
  private Result run(final RegistrySyncState base) {
    final Validity validity = new Validity();
    long since = base.seq();
    Expirable<StateChanges> page = this.client.getChanges(this.instanceId, since);
    validity.add(page);
    final boolean full = page.getValue().full();
    if (full && since > 0) {
      // Only the pages from seq 0 hold every live key
      return null;
    }
    final TreeMap<String, String> pending = full ? new TreeMap<>() : new TreeMap<>(base.records());
    long applied = base.seq();
    boolean changed = full;
    while (true) {
      final StateChanges changes = page.getValue();
      final List<StateChanges.Change> ordered = Optional.ofNullable(changes.changes()).orElseGet(List::of).stream()
          .sorted(Comparator.comparingLong(StateChanges.Change::seq))
          .toList();
      for (final StateChanges.Change change : ordered) {
        switch (change.op()) {
          case StateChanges.UPSERT -> {
            if (change.record() == null) {
              throw new IllegalStateException("Missing record for upsert of key %s at seq %d"
                  .formatted(change.key(), change.seq()));
            }
            pending.put(change.key(), change.record());
          }
          case StateChanges.DELETE -> pending.remove(change.key());
          default -> throw new IllegalStateException("Unknown change operation '%s' for key %s"
              .formatted(change.op(), change.key()));
        }
        applied = change.seq();
        changed = true;
      }
      if (changes.next() == null) {
        break;
      }
      if (changes.next() <= since) {
        throw new IllegalStateException("Registry returned next %d that does not advance from since %d"
            .formatted(changes.next(), since));
      }
      since = changes.next();
      page = this.client.getChanges(this.instanceId, since);
      if (page.getValue().full() && !full) {
        return null;
      }
      validity.add(page);
    }
    if (!changed) {
      return new Result(base, false, validity);
    }
    return new Result(new RegistrySyncState(applied, pending), true, validity);
  }

  /**
   * Result of a sync run.
   *
   * @param state    resulting state
   * @param changed  true if the state differs from the active state
   * @param validity of the pages applied
   */
  private record Result(RegistrySyncState state, boolean changed, Validity validity) {
  }

  /**
   * Earliest expiry and issue time of the pages applied.
   */
  private static final class Validity {
    private Instant expiration;
    private Instant issuedAt;

    private void add(final Expirable<?> response) {
      if (this.expiration == null || response.getExpiration().isBefore(this.expiration)) {
        this.expiration = response.getExpiration();
      }
      if (this.issuedAt == null || response.getIssuedAt().isBefore(this.issuedAt)) {
        this.issuedAt = response.getIssuedAt();
      }
    }
  }
}
