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
package se.swedenconnect.oidf.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import lombok.AllArgsConstructor;

import java.lang.reflect.Type;
import java.text.ParseException;

/**
 * Implements {@link JsonDeserializer} and {@link JsonSerializer} for JWK.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class ReferenceJWKSSerializer implements JsonDeserializer<JWKSet>, JsonSerializer<JWKSet> {

  private final JWKSPropertyLoader jwkPropertyLoader;

  @Override
  public JWKSet deserialize(
      final JsonElement jsonElement,
      final Type type,
      final JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {
    try {
      if (jsonElement.isJsonObject()) {
        final String asString = jsonElement.getAsJsonObject().toString();
        return JWKSet.parse(asString);
      }
      else {
        final String asString = jsonElement.getAsString();
        return this.jwkPropertyLoader.convert(asString);
      }

    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JsonElement serialize(
      final JWKSet jwk,
      final Type type,
      final JsonSerializationContext jsonSerializationContext) {

    final JsonObject jsonObject = new JsonObject();
    jwk.toPublicJWKSet().toJSONObject()
        .forEach((k,v)-> {
          jsonObject.addProperty(k, (String) v);
        });

    return jsonObject;
  }
}
