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
import se.swedenconnect.oidf.common.entity.entity.integration.registry.LocalRegistryProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.List;
import java.util.Optional;

/**
 * ConfigurationProperties based {@link RecordSource} implementation for local records.
 *
 * @author Felix Hellman
 */
public class LocalRecordSource implements RecordSource {

  private final LocalRegistryProperties properties;

  /**
   * Constructor.
   * @param properties to get records from
   */
  public LocalRecordSource(final LocalRegistryProperties properties) {
    this.properties = properties;
  }

  @Override
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return this.properties.trustMarkIssuerProperties();
  }

  @Override
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return this.properties.trustAnchorProperties();
  }

  @Override
  public List<ResolverProperties> getResolverProperties() {
    return this.properties.resolverProperties();
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.properties.entityRecords().stream()
        .filter(er -> er.getEntityIdentifier().getValue().equals(key.issuer()))
        .findFirst();
  }

  @Override
  public List<EntityRecord> getAllEntities() {
    return this.properties.entityRecords();
  }

  @Override
  public List<TrustAnchorProperties.SubordinateListingProperty> findSubordinates(final String issuer) {
    return this.properties.trustAnchorProperties()
        .stream()
        .filter(ta -> ta.getEntityIdentifier().getValue().equals(issuer))
        .flatMap(ta -> ta.getSubordinates().stream())
        .toList();
  }

  @Override
  public List<TrustMarkSubjectProperty> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return this.properties
        .trustMarkIssuerProperties().stream().filter(tmi -> tmi.entityIdentifier().equals(issuer))
        .flatMap(tmi -> tmi.trustMarks().stream())
        .filter(tm -> tm.getTrustMarkId().equals(id))
        .flatMap(tm -> tm.getTrustMarkSubjects().stream())
        .toList();
  }

  @Override
  public Optional<TrustMarkSubjectProperty> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
      final EntityID subject) {
    return this.getTrustMarkSubjects(issuer, id).stream()
        .filter(tms -> tms.sub().equals(subject.getValue()))
        .findFirst();
  }

  @Override
  public int priority() {
    return 0;
  }
}
