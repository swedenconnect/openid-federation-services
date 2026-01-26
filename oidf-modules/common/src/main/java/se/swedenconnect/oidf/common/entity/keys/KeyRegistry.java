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
package se.swedenconnect.oidf.common.entity.keys;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for holding JWKs by alias
 *
 * @author Felix Hellman
 */
public class KeyRegistry {

  private final Map<String, JWK> aliasToJwkMap = new HashMap<>();
  private final Map<String, JWK> kidToJwkMap = new HashMap<>();

  /**
   * Gets a single key from registry.
   * @param alias to fetch
   * @return jwk
   */
  public Optional<JWK> getKey(final String alias) {
    return Optional.ofNullable(this.aliasToJwkMap.get(alias));
  }

  /**
   * Gets a single key from registry.
   * @param kid to fetch
   * @return jwk
   */
  public Optional<JWK> getKeyByKid(final String kid) {
    return Optional.ofNullable(this.kidToJwkMap.get(kid));
  }

  /**
   *
   * @return jwk
   */
  public JWK getDefaultKey() {
    return this.aliasToJwkMap.values().stream()
        .filter(JWK::isPrivate)
        .findFirst()
        .get();
  }

  /**
   * @param aliases to find
   * @return a set of jwks matching the aliases provided
   */
  public JWKSet getSet(final List<String> aliases) {
    if (!this.aliasToJwkMap.keySet().containsAll(aliases)) {
      final String message = "Key registry does not contain all requested keys in %s".formatted(aliases);
      throw new IllegalArgumentException(message);
    }

    final List<JWK> list = this.aliasToJwkMap.entrySet()
        .stream()
        .filter(es -> aliases.contains(es.getKey()))
        .map(Map.Entry::getValue)
        .toList();
    return new JWKSet(list);
  }

  /**
   * Registers a key to the registry.
   * @param property to register
   */
  public void register(final KeyProperty property) {
    try {
      this.aliasToJwkMap.put(property.getAlias(), property.getKey());
      if (property.getKey().isPrivate()) {
        this.kidToJwkMap.put(property.getKey().getKeyID(), property.getKey());
      }
    }
    catch (final Exception e) {
      throw new IllegalArgumentException("Failed to add key to registry ", e);
    }
  }

  /**
   * Returns all public keys for all registered private keys.
   * @return jwks
   */
  public JWKSet getAllPublic() {
    return new JWKSet(this.aliasToJwkMap.values().stream().toList()).toPublicJWKSet();
  }
}
