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
package se.digg.oidfed.service.modules;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;
import se.digg.oidfed.resolver.ResolverProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class for resolver modules from registry.
 *
 * @author Felix Hellman
 */
@Getter
public class ResolverModuleResponse {
  private List<String> trustAnchors;
  private Duration resolveResponseDuration;
  private JWKSet trustedKeys;
  private String entityIdentifier;
  private JWK signKey;
  private Duration stepRetryTime;
  private String alias;
  private Boolean active;

  /**
   * Converts this instance to a json object {@link HashMap}
   *
   * @return json object
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put("trust-anchors", this.trustAnchors);
    json.put("resolve-response-duration", this.resolveResponseDuration);
    json.put("trusted-keys", this.trustedKeys);
    json.put("entity-identifier", this.entityIdentifier);
    json.put("sign-key", this.signKey);
    json.put("step-retry-time", this.stepRetryTime);
    json.put("alias", this.alias);
    json.put("active", this.active);
    return Collections.unmodifiableMap(json);
  }

  /**
   * Creates new instance from a json object {@link HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ResolverModuleResponse fromJson(final Map<String, Object> json) {
    final ResolverModuleResponse resolver = new ResolverModuleResponse();
    final Boolean isModuleActive = (Boolean) json.get("active");
    resolver.active = false;
    if (isModuleActive) {
      resolver.trustAnchors = (List<String>) json.get("trust-anchors");
      resolver.resolveResponseDuration = (Duration) json.get("resolve-response-duration");
      resolver.trustedKeys = (JWKSet) json.get("trusted-keys");
      resolver.entityIdentifier = (String) json.get("entity-identifier");
      resolver.signKey = (JWK) json.get("sign-key");
      resolver.stepRetryTime = (Duration) json.get("step-retry-time");
      resolver.alias = (String) json.get("alias");
    }
    return resolver;
  }

  /**
   * Convert this instance to resolver properties
   *
   * @return new instance
   */
  public ResolverProperties toProperty() {
    return new ResolverProperties(
        this.trustAnchors.get(0),
        this.resolveResponseDuration,
        this.trustedKeys.getKeys(),
        this.entityIdentifier,
        this.signKey,
        this.stepRetryTime,
        this.alias);
  }
}