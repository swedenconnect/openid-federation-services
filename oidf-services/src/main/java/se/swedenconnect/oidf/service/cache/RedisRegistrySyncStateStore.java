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
package se.swedenconnect.oidf.service.cache;

import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.sync.RegistrySyncState;
import se.swedenconnect.oidf.sync.RegistrySyncStateStore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis implementation of {@link RegistrySyncStateStore}, so that the node holding the registry lock can continue
 * from the state synced by another node.
 *
 * <p>Each state is written under a new version: the records to the hash
 * {@code registry-sync:<instance>:<version>:records} and the sequence number to
 * {@code registry-sync:<instance>:<version>:seq}. The version is set as
 * {@code registry-sync:<instance>:pending-version} while it is written, and becomes active when
 * {@code registry-sync:<instance>:version} is flipped to it. The previous version is left to expire.</p>
 *
 * @author Felix Hellman
 */
public class RedisRegistrySyncStateStore implements RegistrySyncStateStore {

  private static final Duration PENDING_TTL = Duration.ofMinutes(10);
  private static final Duration RETIRED_TTL = Duration.ofMinutes(5);
  private static final int WRITE_BATCH_SIZE = 1000;

  private final RedisTemplate<String, String> recordTemplate;
  private final RedisTemplate<String, Long> versionTemplate;
  private final Map<UUID, LocalCopy> localCopies = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param recordTemplate  for the record hashes, with string hash keys and values
   * @param versionTemplate for version and sequence numbers
   */
  public RedisRegistrySyncStateStore(
      final RedisTemplate<String, String> recordTemplate,
      final RedisTemplate<String, Long> versionTemplate) {

    this.recordTemplate = recordTemplate;
    this.versionTemplate = versionTemplate;
  }

  @Override
  public Optional<Long> seq(final UUID instanceId) {
    final Long version = this.versionTemplate.opsForValue().get(versionKey(instanceId));
    if (version == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.versionTemplate.opsForValue().get(seqKey(instanceId, version)));
  }

  @Override
  public Optional<RegistrySyncState> load(final UUID instanceId) {
    final Long version = this.versionTemplate.opsForValue().get(versionKey(instanceId));
    if (version == null) {
      return Optional.empty();
    }
    final LocalCopy local = this.localCopies.get(instanceId);
    if (local != null && local.version() == version) {
      return Optional.of(local.state());
    }
    final Long seq = this.versionTemplate.opsForValue().get(seqKey(instanceId, version));
    if (seq == null) {
      return Optional.empty();
    }
    final TreeMap<String, String> records = new TreeMap<>();
    this.recordTemplate.opsForHash().entries(recordsKey(instanceId, version))
        .forEach((key, value) -> records.put(key.toString(), value.toString()));
    final RegistrySyncState state = new RegistrySyncState(seq, records);
    this.localCopies.put(instanceId, new LocalCopy(version, state));
    return Optional.of(state);
  }

  @Override
  public void store(final UUID instanceId, final RegistrySyncState state) {
    final Long current = this.versionTemplate.opsForValue().get(versionKey(instanceId));
    final long version = Math.max(Instant.now().toEpochMilli(), Optional.ofNullable(current).orElse(0L) + 1);
    this.versionTemplate.opsForValue().set(pendingVersionKey(instanceId), version);

    final String recordsKey = recordsKey(instanceId, version);
    final String seqKey = seqKey(instanceId, version);
    final List<Map.Entry<String, String>> entries = new ArrayList<>(state.records().entrySet());
    for (int start = 0; start < entries.size(); start += WRITE_BATCH_SIZE) {
      final Map<String, String> batch = new HashMap<>();
      entries.subList(start, Math.min(start + WRITE_BATCH_SIZE, entries.size()))
          .forEach(entry -> batch.put(entry.getKey(), entry.getValue()));
      this.recordTemplate.opsForHash().putAll(recordsKey, batch);
    }
    this.versionTemplate.opsForValue().set(seqKey, state.seq());
    // Pending keys expire unless the version is flipped to, so that an interrupted write does not leak
    this.recordTemplate.expire(recordsKey, PENDING_TTL);
    this.versionTemplate.expire(seqKey, PENDING_TTL);

    this.versionTemplate.opsForValue().set(versionKey(instanceId), version);
    this.recordTemplate.persist(recordsKey);
    this.versionTemplate.persist(seqKey);
    if (current != null) {
      this.recordTemplate.expire(recordsKey(instanceId, current), RETIRED_TTL);
      this.versionTemplate.expire(seqKey(instanceId, current), RETIRED_TTL);
    }
    this.localCopies.put(instanceId, new LocalCopy(version, state));
  }

  private static String versionKey(final UUID instanceId) {
    return "registry-sync:%s:version".formatted(instanceId);
  }

  private static String pendingVersionKey(final UUID instanceId) {
    return "registry-sync:%s:pending-version".formatted(instanceId);
  }

  private static String recordsKey(final UUID instanceId, final long version) {
    return "registry-sync:%s:%d:records".formatted(instanceId, version);
  }

  private static String seqKey(final UUID instanceId, final long version) {
    return "registry-sync:%s:%d:seq".formatted(instanceId, version);
  }

  /**
   * State last read or written by this node.
   *
   * @param version of the state in redis
   * @param state   the state
   */
  private record LocalCopy(long version, RegistrySyncState state) {
  }
}
