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

import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;

import java.text.ParseException;
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
      resolver.trustAnchors = List.of( (String) json.get("trust-anchor"));
      resolver.resolveResponseDuration = Duration.parse((String) json.get("resolve-response-duration"));

      resolver.entityIdentifier = (String) json.get("entity-identifier");
      resolver.stepRetryTime = Duration.parse((String) json.get("step-retry-duration")); // changed from step-retry-time
      resolver.alias = (String) json.get("alias");
      try {
        resolver.trustedKeys = JWKSet.parse((String) json.get("trusted-keys"));
      }
      catch (ParseException e) {
        throw new RegistryResponseException("Unable to parse trusted-keys in to a JWKSet.",e);
      }

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
        this.trustAnchors.getFirst(),
        this.resolveResponseDuration,
        this.trustedKeys.getKeys(),
        this.entityIdentifier,
        this.stepRetryTime,
        this.alias);
  }
}