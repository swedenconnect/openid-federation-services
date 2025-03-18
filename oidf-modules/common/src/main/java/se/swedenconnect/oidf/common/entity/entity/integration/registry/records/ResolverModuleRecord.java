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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryResponseException;

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
public class ResolverModuleRecord {

  private List<String> trustAnchors;
  private Duration resolveResponseDuration;
  private JWKSet trustedKeys;
  private String entityIdentifier;
  private Duration stepRetryTime;

  /**
   * Converts this instance to a json object {@link HashMap}
   *
   * @return json object
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put(RecordFields.ResolverModule.TRUST_ANCHORS, this.trustAnchors);
    json.put(RecordFields.ResolverModule.RESOLVE_RESPONSE_DURATION, this.resolveResponseDuration);
    json.put(RecordFields.ResolverModule.TRUSTED_KEYS, this.trustedKeys);
    json.put(RecordFields.ResolverModule.ENTITY_IDENTIFIER, this.entityIdentifier);
    json.put(RecordFields.ResolverModule.STEP_RETRY_TIME, this.stepRetryTime);
    return Collections.unmodifiableMap(json);
  }

  /**
   * Creates new instance from a json object {@link HashMap}
   *
   * @param json to read
   * @return new instance
   */
  public static ResolverModuleRecord fromJson(final Map<String, Object> json) {
    final ResolverModuleRecord resolver = new ResolverModuleRecord();
    resolver.trustAnchors = List.of((String) json.get(RecordFields.ResolverModule.TRUST_ANCHORS));
    resolver.resolveResponseDuration =
        Duration.parse((String) json.get(RecordFields.ResolverModule.RESOLVE_RESPONSE_DURATION));
    resolver.entityIdentifier = (String) json.get(RecordFields.ResolverModule.ENTITY_IDENTIFIER);
    resolver.stepRetryTime = Duration.parse((String) json.get(RecordFields.ResolverModule.STEP_RETRY_TIME));
    try {
      resolver.trustedKeys = JWKSet.parse((String) json.get(RecordFields.ResolverModule.TRUSTED_KEYS));
    } catch (final ParseException e) {
      throw new RegistryResponseException("Unable to parse trusted-keys in to a JWKSet.", e);
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
        this.stepRetryTime);
  }
}