/*
 * Copyright 2024-2026 Sweden Connect
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
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for holding JWKs by alias
 *
 * @author Felix Hellman
 */
@Slf4j
public class KeyRegistry {

  private final Map<String, JWK> mappedKey = new HashMap<>();

  /**
   * Gets a single key from registry.
   * @param reference to fetch
   * @return jwk
   */
  public Optional<JWK> getKey(final String reference) {
    return Optional.ofNullable(this.mappedKey.get(reference));
  }
  /**
   *
   * @return jwk
   */
  public Optional<JWK> getDefaultKey() {
    return this.mappedKey.entrySet()
        .stream()
        .filter(kv -> kv.getKey().startsWith("hosted"))
        .map(Map.Entry::getValue)
        .filter(JWK::isPrivate)
        .findFirst();
  }

  /**
   * @param mapping to find
   * @return a set of jwks matching the mappings provided
   */
  public JWKSet getByReferences(final List<String> mapping) {
    if (!this.mappedKey.keySet().containsAll(mapping)) {
      log.error("Key registry does not contain all requested keys in {}", mapping);
    }

    final List<JWK> list = this.mappedKey.entrySet()
        .stream()
        .filter(es -> mapping.contains(es.getKey()))
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
      if (Objects.nonNull(property.getMapping())) {
        property.getMapping().forEach(mapping -> {
          this.mappedKey.put(
              "%s:%s".formatted(mapping, property.getKey().getKeyID()),
              property.getKey()
          );
          this.mappedKey.put(
              "%s:%s".formatted(mapping, property.getAlias()),
              property.getKey()
          );
        });
      }
    } catch (final Exception e) {
      throw new IllegalArgumentException("Failed to add key to registry ", e);
    }
  }

  /**
   * Returns all public keys for all registered private keys.
   * @return jwks
   */
  public Map<String, JWKSet> getMappedPublicKeys() {
    final List<JWK> federationKeys = this.mappedKey.entrySet().stream()
        .filter((kv) -> kv.getKey().startsWith("federation:"))
        //Filter on key id so we don't get the same key for alias + keyid
        .filter(kv -> kv.getKey().endsWith(kv.getValue().getKeyID()))
        .map(Map.Entry::getValue)
        .map(JWK::toPublicJWK)
        .toList();

    final List<JWK> hostedKeys = this.mappedKey.entrySet().stream()
        .filter((kv) -> kv.getKey().startsWith("hosted:"))
        //Filter on key id so we don't get the same key for alias + keyid
        .filter(kv -> kv.getKey().endsWith(kv.getValue().getKeyID()))
        .map(Map.Entry::getValue)
        .map(JWK::toPublicJWK)
        .toList();

    return Map.of(
        "hosted", new JWKSet(hostedKeys),
        "federation", new JWKSet(federationKeys)
        );
  }

  /**
   * Gets the public key alias names grouped by usage.
   *
   * @return a map with keys "hosted" and "federation" mapping to their respective key alias names
   */
  public Map<String, List<String>> getPublicKeyNames() {
    final List<String> federationKeyNames = this.mappedKey.entrySet().stream()
        .filter((kv) -> kv.getKey().startsWith("federation:"))
        //Filter the keys that do not contain kid so we can extract alias.
        .filter(kv -> !kv.getKey().endsWith(kv.getValue().getKeyID()))
        .map(Map.Entry::getKey)
        .toList();

    final List<String> hostedKeyNames = this.mappedKey.entrySet().stream()
        .filter((kv) -> kv.getKey().startsWith("hosted:"))

        .filter(kv -> !kv.getKey().endsWith(kv.getValue().getKeyID()))
        .map(Map.Entry::getKey)
        .toList();

    return Map.of(
        "hosted", hostedKeyNames,
        "federation", federationKeyNames
    );
  }

  /**
   * @param kid to find mapping for
   * @return first available mapping of key
   */
  public String getMapping(final String kid) {
    final Optional<String> mapping =
        this.mappedKey.entrySet().stream().filter(kv -> kv.getKey().contains(kid))
            .map(Map.Entry::getKey)
            .findFirst();
    return mapping.orElse("mapping-missing");
  }
}
