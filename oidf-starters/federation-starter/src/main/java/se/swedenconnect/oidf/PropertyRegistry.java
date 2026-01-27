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
package se.swedenconnect.oidf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.LocalRegistryProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;
import java.util.Objects;

/**
 * Registry based on properties.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PropertyRegistry {

  private List<TrustMarkIssuerProperties> trustMarkIssuers;

  private List<TrustAnchorProperties> trustAnchors;

  private List<ResolverProperties> resolvers;

  private List<EntityRecord> entities;


  /**
   * Converts this to internal property based entity.
   * @return this converted
   */
  public LocalRegistryProperties toProperty() {
    return new LocalRegistryProperties(
        this.trustMarkIssuers,
        this.trustAnchors,
        this.resolvers,
        this.entities
    );
  }

  /**
   * Validates configuration propety
   * @param key of parent
   */
  public void validate(final String key) {
    this.resolvers.forEach(r -> {
      final String entityIdentifier = r.getEntityIdentifier();
      if (!this.entityExists(entityIdentifier)) {
        throw new IllegalArgumentException("%s.%s defines %s but could not be found in %s.%s"
            .formatted(
                key, "resolvers",
                entityIdentifier,
                key, "entities"
            ));
      }
    });
    this.trustAnchors.forEach(r -> {
      final String entityIdentifier = r.getEntityIdentifier().getValue();
      if (!this.entityExists(entityIdentifier)) {
        throw new IllegalArgumentException("%s.%s defines %s but could not be found in %s.%s"
            .formatted(
                key, "trust-anchors",
                entityIdentifier,
                key, "entities"
            ));
      }
    });
    this.trustMarkIssuers.forEach(r -> {
      final String entityIdentifier = r.entityIdentifier().getValue();
      if (!this.entityExists(entityIdentifier)) {
        throw new IllegalArgumentException("%s.%s defines %s but could not be found in %s.%s"
            .formatted(
                key, "trust-mark-issuers",
                entityIdentifier,
                key, "entities"
            ));
      }
    });
  }

  private boolean entityExists(final String entityId) {
    if (Objects.isNull(this.entities) || this.entities.isEmpty()) {
      return false;
    }
    return this.entities.stream().anyMatch(e -> e.getEntityIdentifier().getValue()
        .equals(entityId));
  }
}
