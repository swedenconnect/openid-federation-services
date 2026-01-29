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
import java.util.stream.Collectors;

/**
 * JWK kid reference loader.
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
    final List<String> references = Arrays.stream(jwksReference.split(",")).toList();
    final JWKSet byReferences = this.registry.getByReferences(references);
    if (byReferences.isEmpty()) {
      log.warn("Reference %s contained no valid keys, loading default ...".formatted(jwksReference));
      return new JWKSet(this.registry.getDefaultKey());
    }
    return byReferences;
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
            .collect(Collectors.joining(","))
    );
  }
}
