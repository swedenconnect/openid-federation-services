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
package se.digg.oidfed.common.entity.integration.registry;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TrustAnchor Module from Registry.
 *
 * @author Felix Hellman
 */
@Getter
public class TrustAnchorModuleRecord {
  /** EntityId for the trust anchor */
  private String entityIdentifier;
  /** Is the module qctive */
  private Boolean active;

  /**
   * Converts this instance to json object {@link HashMap}
   * @return json object
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put("entity-identifier", this.entityIdentifier);
    json.put("active", this.active);
    return Collections.unmodifiableMap(json);
  }

  /**
   * Converts json object to new instance.
   * @param json to read
   * @return new instance
   */
  public static TrustAnchorModuleRecord fromJson(final Map<String, Object> json) {
    final TrustAnchorModuleRecord trustAnchorModuleRecord = new TrustAnchorModuleRecord();
    trustAnchorModuleRecord.entityIdentifier = (String) json.get("entity-identifier");
    trustAnchorModuleRecord.active = (Boolean) json.get("active");
    return trustAnchorModuleRecord;
  }

  /**
   * Concerts response to properties.
   * @return properties instance
   */
  public TrustAnchorProperties toProperties() {
    return new TrustAnchorProperties(new EntityID(this.entityIdentifier));
  }

}
