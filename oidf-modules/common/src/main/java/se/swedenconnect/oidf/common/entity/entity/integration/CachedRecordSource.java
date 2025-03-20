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
package se.swedenconnect.oidf.common.entity.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ResolverModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustAnchorModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkIssuerModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.List;
import java.util.Optional;

/**
 * Cache backed implementation of {@link RecordSource}.
 *
 * @author Felix Hellman
 */
public class CachedRecordSource implements RecordSource {
  private final Cache<String, CompositeRecord> cache;

  /**
   * Constructor.
   *
   * @param cache for saving a single composite record
   */
  public CachedRecordSource(final Cache<String, CompositeRecord> cache) {
    this.cache = cache;
  }

  /**
   * @param record to add
   */
  public void addRecord(final CompositeRecord record) {
    this.cache.add("record", new Expirable<>(record.getExpiration(), record.getIssuedAt(), record));
  }

  private Optional<CompositeRecord> getRecord() {
    return Optional.ofNullable(this.cache.get("record"));
  }

  @Override
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getTrustMarkIssuers()
            .stream()
            .map(TrustMarkIssuerModuleRecord::toProperties)
            .toList()
        ).orElse(List.of());
  }

  @Override
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getTrustAnchors()
            .stream()
            .map(TrustAnchorModuleRecord::toProperties)
            .toList()
        ).orElse(List.of());
  }

  @Override
  public List<ResolverProperties> getResolverProperties() {
    return this.getRecord()
        .map(r -> r
            .getModuleRecord()
            .getValue()
            .getResolvers()
            .stream()
            .map(ResolverModuleRecord::toProperty)
            .toList()
        ).orElse(List.of());
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.getRecord()
        .flatMap(r -> r.getEntityRecords().getValue().stream()
            .filter(er -> er.getIssuer().getValue().equals(key.issuer()))
            .filter(er -> er.getSubject().getValue().equals(key.subject()))
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
  public List<EntityRecord> findSubordinates(final String issuer) {
    return this.getRecord()
        .map(r -> r.getEntityRecords().getValue().stream()
            .filter(record -> !record.getSubject().getValue().equals(issuer))
            .filter(record -> record.getIssuer().getValue().equals(issuer))
            .toList()
        ).orElse(List.of());
  }

  @Override
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return this.getRecord()
        .map(r ->
            r.getTrustMarkRecords().getValue().stream()
                .filter(record -> record.getTrustMarkId().equals(id.getTrustMarkId()))
                .filter(record -> record.getTrustMarkIssuerId().equals(issuer.getValue()))
                .flatMap(record -> record.getSubjects().stream())
                .toList()
        ).orElse(List.of());

  }

  @Override
  public Optional<TrustMarkSubjectRecord> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
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
