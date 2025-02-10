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
package se.digg.oidfed.service.modules;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;
import se.digg.oidfed.trustanchor.TrustAnchorProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TrustAnchor Module from Registry.
 *
 * @author Felix Hellman
 */
@Getter
public class TrustAnchorModuleResponse {
  /** Alias for the given module */
  private String alias;
  /** EntityId for the trust anchor */
  private EntityID entityIdentifier;
  /** Is the module qctive */
  private Boolean active;
  /** Base path for trust anchor */
  private String basePath;

  /**
   * Converts this instance to json object {@link HashMap}
   * @return json object
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put("alias", this.alias);
    json.put("entity-identifier", this.entityIdentifier);
    json.put("active", this.active);
    return Collections.unmodifiableMap(json);
  }

  /**
   * Converts json object to new instance.
   * @param json to read
   * @return new instance
   */
  public static TrustAnchorModuleResponse fromJson(final Map<String, Object> json) {
    try {
      final TrustAnchorModuleResponse trustAnchorModuleResponse = new TrustAnchorModuleResponse();
      trustAnchorModuleResponse.alias = (String) json.get("alias");
      trustAnchorModuleResponse.entityIdentifier = EntityID.parse((String) json.get("entity-identifier"));
      trustAnchorModuleResponse.active = (Boolean) json.get("active");
      return trustAnchorModuleResponse;
    }
    catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse data from registry",e);
    }
  }

  /**
   * Concerts response to properties.
   * @return properties instance
   */
  public TrustAnchorProperties toProperties() {
    return new TrustAnchorProperties(this.alias, this.entityIdentifier, this.basePath);
  }

}
