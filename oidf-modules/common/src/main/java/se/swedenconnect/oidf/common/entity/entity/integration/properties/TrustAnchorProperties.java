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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Property class for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ToString
public class TrustAnchorProperties {
  private final EntityID entityId;

  private final Map<EntityID, List<Issuer>> trustMarkIssuers;

  private final Map<EntityID, TrustMarkOwner> trustMarkOwners;

  private final ConstraintRecord constraintRecord;

  /**
   * Constructor.
   *
   * @param entityId of the trust anchor
   * @param trustMarkIssuers that are valid for this trust anchor
   * @param constraintRecord for constraints
   * @param trustMarkOwners to trust
   */
  public TrustAnchorProperties(
      final EntityID entityId,
      final Map<EntityID, List<Issuer>> trustMarkIssuers,
      final ConstraintRecord constraintRecord,
      final Map<EntityID, TrustMarkOwner> trustMarkOwners) {
    this.entityId = entityId;
    this.trustMarkIssuers = trustMarkIssuers;
    this.constraintRecord = constraintRecord;
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
