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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.ListCache;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In memory implementation of {@link EntityRecordRegistry}
 *
 * @author Felix Hellman
 */
public class CachedEntityRecordRegistry implements EntityRecordRegistry {
  private final Cache<String, EntityRecord> entityRecordCache;
  private final ListCache<String, String> entityIds;
  private final EntityPathFactory factory;
  private final String instanceId;

  /**
   * Constructor.
   * @param entityRecordCache cache
   * @param entityIds listcache
   * @param factory for creating entity paths
   * @param instanceId of this instance (group)
   */
  public CachedEntityRecordRegistry(
      final Cache<String, EntityRecord> entityRecordCache,
      final ListCache<String, String> entityIds,
      final EntityPathFactory factory,
      final String instanceId) {

    this.entityRecordCache = entityRecordCache;
    this.entityIds = entityIds;
    this.factory = factory;
    this.instanceId = instanceId;
  }

  @Override
  public Optional<EntityRecord> getEntity(final String path) {
    return Optional.ofNullable(this.entityRecordCache.get("%s:%s:path".formatted(this.instanceId, path)));
  }

  @Override
  public Set<String> getPaths() {
    return new HashSet<>(this.entityIds.getAll("%s:paths".formatted(this.instanceId)));
  }

  @Override
  public Optional<EntityRecord> getEntity(final EntityID subject) {
    return Optional.ofNullable(this.entityRecordCache.get("%s:%s:er".formatted(this.instanceId, subject)));
  }

  @Override
  public Optional<EntityRecord> getSubordinateRecord(final EntityID subject) {
    return Optional.ofNullable(this.entityRecordCache.get("%s:%s:ers".formatted(this.instanceId, subject)));
  }

  @Override
  public void addEntity(final EntityRecord record) {
    Optional.ofNullable(record.getHostedRecord()).ifPresent(hostedRecord -> {
      //By subject
      final String subject = record.getSubject().getValue();
      this.entityRecordCache.add("%s:%s:er".formatted(this.instanceId, subject), Expirable.nonExpiring(record));
      this.entityIds.append("%s:entities".formatted(this.instanceId), subject);
      //By path
      final String path = this.factory.getPath(record);
      this.entityRecordCache.add("%s:%s:path".formatted(this.instanceId, path), Expirable.nonExpiring(record));
      this.entityIds.append("%s:paths".formatted(this.instanceId), path);
      if (!record.getSubject().equals(record.getIssuer())) {
        this.entityRecordCache.add("%s:%s:ers".formatted(this.instanceId, subject), Expirable.nonExpiring(record));
        this.entityIds.append("%s:%s:sub".formatted(
                this.instanceId, record.getIssuer()),
            record.getSubject().getValue());
      }
    });
    if (Objects.isNull(record.getHostedRecord())) {
      final String subject = record.getSubject().getValue();
      this.entityRecordCache.add("%s:%s:ers".formatted(this.instanceId, subject), Expirable.nonExpiring(record));
      this.entityIds.append("%s:%s:sub".formatted(
          this.instanceId, record.getIssuer()),
          record.getSubject().getValue());
    }
  }

  @Override
  public List<EntityRecord> findSubordinates(
      final String issuer) {
    final String key = "%s:%s:sub".formatted(this.instanceId, issuer);
    return this.entityIds.getAll(key).stream()
        .map(subject -> this.getSubordinateRecord(new EntityID(subject)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        // Remove self from listing
        .filter(er -> !er.getSubject().getValue().equals(issuer))
        .toList();
  }
}
