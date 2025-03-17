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
package se.digg.oidfed.common.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.properties.ResolverProperties;
import se.digg.oidfed.common.entity.integration.properties.TrustAnchorProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkId;
import se.digg.oidfed.common.entity.integration.properties.TrustMarkIssuerProperties;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Combines multiple record sources and sorts them by priority.
 *
 * @author Felix Hellman
 */
public class CompositeRecordSource implements RecordSource {
  private final List<RecordSource> recordSources;

  /**
   * Constructor.
   * @param recordSources to handle.
   */
  public CompositeRecordSource(final List<RecordSource> recordSources) {
    final ArrayList<RecordSource> tmp = new ArrayList<>(recordSources);
    tmp.sort(Comparator.comparingInt(RecordSource::priority));
    this.recordSources = List.copyOf(tmp);
  }

  @Override
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return this.recordSources.stream()
        .flatMap(r -> r.getTrustMarkIssuerProperties().stream())
        .toList();
  }

  @Override
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return this.recordSources.stream()
        .flatMap(r -> r.getTrustAnchorProperties().stream())
        .toList();
  }

  @Override
  public List<ResolverProperties> getResolverProperties() {
    return this.recordSources.stream()
        .flatMap(r -> r.getResolverProperties().stream())
        .toList();
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.recordSources.stream()
        .map(s -> s.getEntity(key))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  @Override
  public List<EntityRecord> getAllEntities() {
    return this.recordSources.stream()
        .flatMap(r -> r.getAllEntities().stream())
        .toList();
  }

  @Override
  public List<EntityRecord> findSubordinates(final String issuer) {
    return this.recordSources.stream()
        .flatMap(r -> r.findSubordinates(issuer).stream())
        .toList();
  }

  @Override
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return this.recordSources.stream()
        .flatMap(r -> r.getTrustMarkSubjects(issuer, id).stream())
        .toList();
  }

  @Override
  public Optional<TrustMarkSubjectRecord> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
      final EntityID subject) {
    return this.recordSources.stream()
        .map(s -> s.getTrustMarkSubject(issuer, id, subject))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  @Override
  public int priority() {
    return 0;
  }
}
