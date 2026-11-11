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
package se.swedenconnect.oidf.common.entity.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cache backed implementation of {@link RecordSource}.
 *
 * @author Felix Hellman
 */
public class CachedRecordSource implements RecordSource {

  private static final Duration TTL = Duration.ofSeconds(10);

  private final Cache<String, CompositeRecord> cache;
  private final Duration ttl;
  private final ReentrantLock refreshLock = new ReentrantLock();
  private volatile CompositeRecord localRecord;
  private volatile Instant localRecordFetchedAt = Instant.MIN;

  /**
   * Constructor.
   *
   * @param cache for saving a single composite record
   */
  public CachedRecordSource(final Cache<String, CompositeRecord> cache) {
    this(cache, TTL);
  }

  /**
   * Constructor.
   *
   * @param cache for saving a single composite record
   * @param ttl   how long the local copy is used before it is refreshed from the cache
   */
  CachedRecordSource(final Cache<String, CompositeRecord> cache, final Duration ttl) {
    this.cache = cache;
    this.ttl = ttl;
  }

  /**
   * @param record to add
   */
  public void addRecord(final CompositeRecord record) {
    this.cache.add("record", new Expirable<>(record.getExpiration(), record.getIssuedAt(), record));
    this.localRecord = record;
    this.localRecordFetchedAt = Instant.now();
  }

  private Optional<CompositeRecord> getRecord() {
    final CompositeRecord current = this.localRecord;
    if (current != null && this.isFresh()) {
      return Optional.of(current);
    }
    if (current != null) {
      if (!this.refreshLock.tryLock()) {
        return Optional.of(current);
      }
    }
    else {
      this.refreshLock.lock();
    }
    try {
      final CompositeRecord refreshed = this.localRecord;
      if (refreshed != null && this.isFresh()) {
        return Optional.of(refreshed);
      }
      final CompositeRecord fetched = this.cache.get("record");
      if (fetched != null) {
        this.localRecord = fetched;
        this.localRecordFetchedAt = Instant.now();
      }
      return Optional.ofNullable(fetched);
    }
    finally {
      this.refreshLock.unlock();
    }
  }

  private boolean isFresh() {
    return Duration.between(this.localRecordFetchedAt, Instant.now()).compareTo(this.ttl) < 0;
  }

  @Override
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getTrustMarkIssuers()
        ).orElse(List.of());
  }

  @Override
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getTrustAnchors()
        ).orElse(List.of());
  }

  @Override
  public List<ResolverProperties> getResolverProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getResolvers()
        ).orElse(List.of());
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.getRecord()
        .flatMap(r -> r.getEntityRecords().getValue().stream()
            .filter(er -> er.getEntityIdentifier().getValue().equals(key.entityId()))
            .findFirst());
  }

  @Override
  public List<EntityRecord> getAllEntities() {
    return this.getRecord()
        .map(r -> r.getEntityRecords().getValue().stream()
            .toList()
        ).orElse(List.of());
  }

  @Override
  public Optional<EntityRecord> getEntityByVirtualEntityId(final EntityID virtualEntityId) {
    return this.getRecord()
        .flatMap(r -> r.getEntityRecords().getValue().stream()
            .filter(er -> er.getVirtualEntityId() != null
                && er.getVirtualEntityId().getValue().equals(virtualEntityId.getValue()))
            .findFirst());
  }

  @Override
  public List<TrustAnchorProperties.SubordinateListingProperty> findSubordinates(final String issuer) {
    return this.getTrustAnchorProperties().stream()
        .filter(ta -> ta.getEntityIdentifier().getValue().equals(issuer))
        .flatMap(ta -> ta.getSubordinates().stream())
        .toList();
  }

  @Override
  public List<TrustMarkSubjectProperty> getTrustMarkSubjects(final EntityID issuer, final TrustMarkType id) {
    return this.getTrustMarkIssuerProperties().stream()
        .filter(tmi -> tmi.entityIdentifier().equals(issuer))
        .flatMap((tmi -> {
          return tmi.trustMarks()
              .stream()
              .filter(tm -> tm.getTrustMarkType().equals(id));
        }))
        .flatMap(tm -> tm.getTrustMarkSubjects().stream())
        .toList();
  }

  @Override
  public Optional<TrustMarkSubjectProperty> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkType id,
      final EntityID subject) {
    return this.getTrustMarkSubjects(issuer, id).stream()
        .filter(record -> record.sub().equals(subject.getValue()))
        .findFirst();
  }

  @Override
  public int priority() {
    return 0;
  }

  /**
   * @return true if the cache should be reloaded
   */
  public boolean shouldRefresh() {
    return this.cache.shouldRefresh("record");
  }
}
