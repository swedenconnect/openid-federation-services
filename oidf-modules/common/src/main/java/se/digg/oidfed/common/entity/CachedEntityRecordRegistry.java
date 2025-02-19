/*
 * Copyright 2024-2025 Sweden Connect
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
package se.digg.oidfed.common.entity;

import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.CacheRequest;
import se.digg.oidfed.common.entity.integration.ListCache;
import se.digg.oidfed.common.entity.integration.MultiKeyCache;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In memory implementation of {@link EntityRecordRegistry}
 *
 * @author Felix Hellman
 */
public class CachedEntityRecordRegistry implements EntityRecordRegistry {
  private final EntityPathFactory factory;
  private final MultiKeyCache<EntityRecord> entityRecords;

  /**
   * Constructor.
   * @param factory for creating paths
   * @param entityRecords cache for entityrecords
   */
  public CachedEntityRecordRegistry(final EntityPathFactory factory, final MultiKeyCache<EntityRecord> entityRecords) {
    this.factory = factory;
    this.entityRecords = entityRecords;
  }

  @Override
  public Optional<EntityRecord> getEntity(final String path) {
    return Optional.ofNullable(this.entityRecords.get(new CacheRequest(path, "paths")));
  }

  @Override
  public Set<String> getPaths() {
    return this.entityRecords.getSubKeys("paths");
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return Optional.ofNullable(this.entityRecords.get(new CacheRequest(key.getKey(), null)));
  }

  @Override
  public void addEntity(final EntityRecord record) {
    final NodeKey key = NodeKey.fromEntityRecord(record);
    if (record.isHosted()) {
      final String path = this.factory.getPath(record);
      this.entityRecords.add(key.getKey(), Map.of("paths", path), record);
    }
    this.entityRecords.add(key.getKey(), record);
  }

  @Override
  public List<EntityRecord> findSubordinates(final String issuer) {
    return this.entityRecords.getPrimaryKeys().stream()
        .map(NodeKey::parse)
        .filter(key -> key.issuer().equals(issuer))
        .filter(key -> !key.isSelfStatement())
        .map(key -> this.entityRecords.get(new CacheRequest(key.getKey(), null)))
        .toList();
  }
}
