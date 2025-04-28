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
package se.swedenconnect.oidf.service.controller;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.RecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Make the docs happy.
 *
 * @author Felix Hellman
 */
public class RegistrationSource implements RecordSource {

  private final List<EntityRecord> registerdEntities;

  /**
   * Default constructor,
   */
  public RegistrationSource() {
    this.registerdEntities = new ArrayList<>();
  }

  /**
   * @param record to add
   */
  public void addEntity(final EntityRecord record) {
    this.registerdEntities.add(record);
  }


  @Override
  public List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties() {
    return List.of();
  }

  @Override
  public List<TrustAnchorProperties> getTrustAnchorProperties() {
    return List.of();
  }

  @Override
  public List<ResolverProperties> getResolverProperties() {
    return List.of();
  }

  @Override
  public Optional<EntityRecord> getEntity(final NodeKey key) {
    return this.registerdEntities.stream()
        .filter(key::matches)
        .findAny();
  }

  @Override
  public List<EntityRecord> getAllEntities() {
    return this.registerdEntities;
  }

  @Override
  public List<EntityRecord> findSubordinates(final String issuer) {
    return List.of();
  }

  @Override
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final EntityID issuer, final TrustMarkId id) {
    return List.of();
  }

  @Override
  public Optional<TrustMarkSubjectRecord> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkId id,
      final EntityID subject) {

    return Optional.empty();
  }

  @Override
  public int priority() {
    return 2;
  }
}
