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
package se.swedenconnect.oidf.service.trustanchor;

import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkOwner;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Properties for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustAnchorModuleProperties {
  /**
   * Property path for this module
   */
  public static final String PROPERTY_PATH = "openid.federation.trust-anchor";

  /**
   * List of all trust anchor modules
   */
  private List<TrustAnchorSubModuleProperties> anchors;

  /**
   * Module properties for an individual trust anchor module
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class TrustAnchorSubModuleProperties {
    /**
     * EntityId for the trust anchor
     */
    private String entityIdentifier;

    private Map<String, List<String>> trustMarkIssuers = Map.of();

    @NestedConfigurationProperty
    private ConstraintProperties constraints;

    @NestedConfigurationProperty
    private List<TrustMarkOwnerProperties> trustMarkOwners;

    /**
     * Converts this to {@link TrustAnchorProperties}
     *
     * @return property
     */
    public TrustAnchorProperties toTrustAnchorProperties() {
      return new TrustAnchorProperties(new EntityID(this.entityIdentifier),
          this.trustMarkIssuers.entrySet().stream().collect(Collectors.toMap(k -> new EntityID(k.getKey()),
              v -> v.getValue().stream().map(Issuer::new).toList())),
          Optional.ofNullable(this.constraints).map(c -> {
                return new ConstraintRecord(
                    this.constraints.getMaxPathLength(),
                    this.constraints.getNaming(),
                    this.constraints.getAllowedEntityTypes()
                );
              })
              .orElse(null),
          Optional.ofNullable(this.trustMarkOwners).map(this::getTrustMarkOwnerMap).orElse(Map.of())
      );
    }

    private Map<EntityID, TrustMarkOwner> getTrustMarkOwnerMap(final List<TrustMarkOwnerProperties> list) {
      return list.stream().collect(Collectors.toMap(v -> new EntityID(v.getTrustMarkId()), v -> {
        try {
          return v.toTrustMarkOwner();
        } catch (final ParseException e) {
          throw new RuntimeException(e);
        }
      }));
    }
  }
}
