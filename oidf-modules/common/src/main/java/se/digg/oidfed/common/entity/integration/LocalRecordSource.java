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
import se.digg.oidfed.common.entity.integration.registry.RegistryProperties;
import se.digg.oidfed.common.entity.integration.properties.ResolverProperties;
import se.digg.oidfed.common.entity.integration.properties.TrustAnchorProperties;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkId;
import se.digg.oidfed.common.entity.integration.properties.TrustMarkIssuerProperties;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Optional;

/**
 * ConfigurationProperties based {@link RecordSource} implementation for local records.
 *
 * @author Felix Hellman
 */
public class LocalRecordSource implements RecordSource {

  private final RegistryProperties properties;

  /**
   * Constructor.
   * @param properties to get records from
   */
  public LocalRecordSource(final RegistryProperties properties) {
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
        .filter(er -> er.getIssuer().getValue().equals(key.issuer()))
        .filter(er -> er.getSubject().getValue().equals(key.subject()))
        .findFirst();
  }

  @Override
  public List<EntityRecord> getAllEntities() {
    return this.properties.entityRecords();
  }

  @Override
  public List<EntityRecord> findSubordinates(final String issuer) {
    return this.properties.entityRecords().stream()
        .filter(er -> !er.getSubject().getValue().equals(issuer))
        .filter(er -> er.getIssuer().getValue().equals(issuer))
        .toList();
  }

  @Override
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return this.properties
        .trustMarkIssuerProperties().stream().filter(tmi -> tmi.issuerEntityId().equals(issuer))
        .flatMap(tmi -> tmi.trustMarks().stream())
        .filter(tm -> tm.trustMarkId().equals(id))
        .flatMap(tm -> tm.trustMarkSubjectRecords().stream())
        .toList();
  }

  @Override
  public Optional<TrustMarkSubjectRecord> getTrustMarkSubject(
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
