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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.Cache;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.Optional;

/**
 * Redis-specific extension of {@link CachedRecordSource} that performs direct O(1) lookups
 * by entity-identifier and virtual-entity-identifier via {@link RedisEntityRecordIndex} instead
 * of loading the full record list and filtering in memory.
 *
 * @author Felix Hellman
 */
public class RedisCachedRecordSource extends CachedRecordSource {

  private final RedisEntityRecordIndex index;

  /**
   * Constructor.
   *
   * @param cache to use for storing the full composite record
   * @param index to use for direct entity lookups
   */
  public RedisCachedRecordSource(
      final Cache<String, CompositeRecord> cache,
      final RedisEntityRecordIndex index) {
    super(cache);
    this.index = index;
  }

  @Override
  public void addRecord(final CompositeRecord record) {
    super.addRecord(record);
    this.index.put(record.getEntityRecords().getValue(), record.getExpiration());
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.index.getByEntityId(key.entityId());
  }

  @Override
  public Optional<EntityRecord> getEntityByVirtualEntityId(final EntityID virtualEntityId) {
    return this.index.getByVirtualEntityId(virtualEntityId.getValue());
  }
}
