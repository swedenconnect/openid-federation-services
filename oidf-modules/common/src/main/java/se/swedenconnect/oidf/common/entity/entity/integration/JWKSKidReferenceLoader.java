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
package se.swedenconnect.oidf.common.entity.entity.integration;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JWK kid reference loader. A reference is a comma separated list of key references, such as
 * {@code hosted:sign-key}, and every key it names must exist in the {@link KeyRegistry}. An empty reference gives
 * the default key.
 *
 * @author Felix Hellman
 */
@Slf4j
@AllArgsConstructor
public class JWKSKidReferenceLoader implements JsonSerializer<JWKSet>, JsonDeserializer<JWKSet> {

  private final KeyRegistry registry;

  @Override
  public JWKSet deserialize(
      final JsonElement jsonElement,
      final Type type,
      final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    final String jwksReference = jsonElement.getAsJsonPrimitive().getAsString();
    final List<String> references = Arrays.stream(jwksReference.split(","))
        .map(String::trim)
        .filter(reference -> !reference.isEmpty())
        .toList();
    if (references.isEmpty()) {
      // An empty reference is treated as no reference, which gives the default key (the first hosted key)
      return this.registry.getDefaultKey()
          .map(JWKSet::new)
          .orElseThrow(() -> new JsonParseException("Empty key reference and no default (hosted) key is configured"));
    }
    final List<String> missing = references.stream()
        .filter(reference -> this.registry.getKey(reference).isEmpty())
        .toList();
    if (!missing.isEmpty()) {
      throw new JsonParseException("Key reference '%s' could not be found".formatted(String.join(",", missing)));
    }
    return this.registry.getByReferences(references);
  }

  @Override
  public JsonElement serialize(
      final JWKSet jwkSet,
      final Type type,
      final JsonSerializationContext jsonSerializationContext) {
    return new JsonPrimitive(
        jwkSet.getKeys()
            .stream()
            .map(JWK::getKeyID)
            .filter(Objects::nonNull)
            .map(this.registry::getMapping)
            .collect(Collectors.joining(","))
    );
  }
}
