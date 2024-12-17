/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Property class for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustAnchorProperties {
  private final String alias;
  private final EntityID entityId;

  /**
   * Constructor.
   * @param alias of the trust anchor
   * @param entityId of the trust anchor
   */
  public TrustAnchorProperties(
      final String alias,
      final EntityID entityId) {
    this.alias = alias;
    this.entityId = entityId;
  }

  /**
   * Entity of an individual subordinate.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class SubordinateListingProperty {
    /**
     * Constructor.
     * @param entityIdentifier for the subordinate
     * @param policy for the subordinate
     */
    public SubordinateListingProperty(final String entityIdentifier, final String policy) {
      this.entityIdentifier = entityIdentifier;
      this.policy = policy;
    }

    private String entityIdentifier;
    private String policy;
  }
}
