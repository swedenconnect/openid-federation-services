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
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.List;
import java.util.Optional;

/**
 * A source of records.
 *
 * @author Felix Hellman
 */
public interface RecordSource {
  /**
   * @return tmi properties
   */
  List<TrustMarkIssuerProperties> getTrustMarkIssuerProperties();

  /**
   * @return trust anchor properties
   */
  List<TrustAnchorProperties> getTrustAnchorProperties();

  /**
   * @return resolver properties
   */
  List<ResolverProperties> getResolverProperties();

  /**
   * @param key for this entity
   * @return entity record if present
   */
  Optional<EntityRecord> getEntity(final NodeKey key);

  /**
   * @return all entities
   */
  List<EntityRecord> getAllEntities();

  /**
   * @param issuer who has subordinates
   * @return list of subordinates
   */
  List<TrustAnchorProperties.SubordinateListingProperty> findSubordinates(final String issuer);

  /**
   * @param issuer of the trust mark
   * @param id of the trust mark
   * @return subjects
   */
  List<TrustMarkSubjectProperty> getTrustMarkSubjects(final EntityID issuer, final TrustMarkType id);

  /**
   * @param issuer of the trust mark
   * @param id of the trust mark
   * @param subject of the trust mark
   * @return a single subject if present
   */
  Optional<TrustMarkSubjectProperty> getTrustMarkSubject(
      final EntityID issuer,
      final TrustMarkType id,
      final EntityID subject);

  /**
   * @return priotiy of this source, lower takes precedence
   */
  int priority();
}
