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
package se.digg.oidfed.service.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import se.digg.oidfed.trustanchor.TrustAnchorProperties;

import java.util.List;

/**
 * Properties for trust anchor.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustAnchorModuleProperties {
  /** Property path for this module */
  public static final String PROPERTY_PATH = "openid.federation.trust-anchor";

  /** Set to true if this module should be active or not. */
  private Boolean active;

  /** List of all trust anchor modules */
  private List<TrustAnchorSubModuleProperties> anchors;

  /**
   * Module properties for an individual trust anchor module
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class TrustAnchorSubModuleProperties {
    /** Alias for the given module */
    private String alias;
    /** EntityId for the trust anchor */
    private String entityIdentifier;
    /** Base path for trust anchor */
    private String basePath;

    /**
     * Converts this to {@link TrustAnchorProperties}
     * @return property
     */
    public TrustAnchorProperties toTrustAnchorProperties() {
      return new TrustAnchorProperties(this.alias, new EntityID(this.entityIdentifier), this.basePath);
    }
  }
}
