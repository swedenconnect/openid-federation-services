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

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Redis Hash-backed index for direct O(1) lookup of {@link EntityRecord} by entity-identifier
 * or virtual-entity-identifier.
 *
 * <p>Two hashes are maintained under the instance-scoped keys
 * {@code entity-record-index} and {@code virtual-entity-index}. Both hashes share the
 * same TTL as the {@link se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord}
 * they were populated from.</p>
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisEntityRecordIndex {

  private static final String ENTITY_HASH_KEY = "entity-record-index";
  private static final String VIRTUAL_HASH_KEY = "virtual-entity-index";

  private final RedisTemplate<String, String> template;
  private final Gson gson;
  private final Clock clock;

  /**
   * Replaces the index contents with the supplied records and sets their TTL.
   *
   * @param records    to index
   * @param expiration when the records expire
   */
  public void put(final List<EntityRecord> records, final Instant expiration) {
    final Duration ttl = Duration.between(Instant.now(this.clock), expiration);

    this.template.delete(ENTITY_HASH_KEY);
    this.template.delete(VIRTUAL_HASH_KEY);

    records.forEach(record -> {
      final String json = this.gson.toJson(record);
      this.template.opsForHash().put(ENTITY_HASH_KEY, record.getEntityIdentifier().getValue(), json);
      if (record.getVirtualEntityId() != null) {
        this.template.opsForHash().put(
            VIRTUAL_HASH_KEY,
            record.getVirtualEntityId().getValue(),
            record.getEntityIdentifier().getValue()
        );
      }
    });

    this.template.expire(ENTITY_HASH_KEY, ttl);
    this.template.expire(VIRTUAL_HASH_KEY, ttl);
  }

  /**
   * Looks up an entity record directly by entity-identifier.
   *
   * @param entityId to look up
   * @return the matching record, or empty if not present
   */
  public Optional<EntityRecord> getByEntityId(final String entityId) {
    final Object json = this.template.opsForHash().get(ENTITY_HASH_KEY, entityId);
    return Optional.ofNullable(json)
        .map(Object::toString)
        .map(j -> this.gson.fromJson(j, EntityRecord.class));
  }

  /**
   * Looks up an entity record directly by virtual-entity-identifier.
   *
   * @param virtualEntityId to look up
   * @return the matching record, or empty if not present
   */
  public Optional<EntityRecord> getByVirtualEntityId(final String virtualEntityId) {
    final Object entityId = this.template.opsForHash().get(VIRTUAL_HASH_KEY, virtualEntityId);
    return Optional.ofNullable(entityId)
        .map(Object::toString)
        .flatMap(this::getByEntityId);
  }
}
