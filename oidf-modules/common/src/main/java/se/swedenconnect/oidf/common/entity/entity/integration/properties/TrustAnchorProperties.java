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
package se.swedenconnect.oidf.common.entity.entity.integration.properties;

import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Property class for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrustAnchorProperties {
  private EntityID entityIdentifier;

  private Map<EntityID, List<Issuer>> trustMarkIssuers;

  private Map<EntityID, TrustMarkOwner> trustMarkOwners;

  private ConstraintRecord constraints;

  /**
   * Constructor.
   *
   * @param entityIdentifier of the trust anchor
   * @param trustMarkIssuers that are valid for this trust anchor
   * @param constraints for constraints
   * @param trustMarkOwners to trust
   */
  public TrustAnchorProperties(
      final EntityID entityIdentifier,
      final Map<EntityID, List<Issuer>> trustMarkIssuers,
      final ConstraintRecord constraints,
      final Map<EntityID, TrustMarkOwner> trustMarkOwners) {
    this.entityIdentifier = entityIdentifier;
    this.trustMarkIssuers = trustMarkIssuers;
    this.constraints = constraints;
    this.trustMarkOwners = trustMarkOwners;
  }

  /**
   * Entity of an individual subordinate.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  @ToString
  public static class SubordinateListingProperty {
    /**
     * Constructor.
     *
     * @param entityIdentifier for the subordinate
     * @param policy           for the subordinate
     */
    public SubordinateListingProperty(final String entityIdentifier, final String policy) {
      this.entityIdentifier = entityIdentifier;
      this.policy = policy;
    }

    private String entityIdentifier;
    private String policy;
  }

  /**
   * @return json of trust mark owners
   */
  public Map<String, Object> trustMarkOwnersJson() {
    if (Objects.isNull(this.trustMarkOwners)) {
      return Map.of();
    }
    return this.getTrustMarkOwners().entrySet().stream()
        .collect(Collectors.toMap(
            k -> k.getKey().getValue(),
            k -> Map.of(
                "sub", k.getValue().getSub(),
                "jwks", k.getValue().getJwks().toJSONObject()
            )
        ));
  }
}
