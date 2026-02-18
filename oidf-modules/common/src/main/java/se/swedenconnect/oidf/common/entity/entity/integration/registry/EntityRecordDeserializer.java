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
package se.swedenconnect.oidf.common.entity.entity.integration.registry;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import se.swedenconnect.oidf.common.entity.entity.integration.DurationDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.InstantDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.JWKSKidReferenceLoader;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkIdentifierDeserializer;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;

/**
 * Deserializer for entity records.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class EntityRecordDeserializer implements JsonDeserializer<EntityRecord> {

  private final JWKSKidReferenceLoader kidReferenceLoader;
  private final KeyRegistry registry;
  private final Gson gson = new GsonBuilder()
      .addDeserializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
          return false;
        }

        @Override
        public boolean shouldSkipClass(final Class<?> aClass) {
          return aClass.equals(JWKSet.class);
        }
      })
      .registerTypeAdapter(Duration.class, new DurationDeserializer())
      .registerTypeAdapter(Instant.class, new InstantDeserializer())
      .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
      .registerTypeAdapter(TrustMarkType.class, new TrustMarkIdentifierDeserializer())
      .create();

  @Override
  public EntityRecord deserialize(
      final JsonElement jsonElement,
      final Type type,
      final JsonDeserializationContext ctx) throws JsonParseException {
    final JsonObject obj = jsonElement.getAsJsonObject();
    final JsonElement jwksElement = obj.remove("jwks");

    final EntityRecord record =  this.gson.fromJson(obj, EntityRecord.class);

    if (jwksElement != null && !jwksElement.isJsonNull()) {
      record.setJwks(this.kidReferenceLoader.deserialize(jwksElement, JWKSet.class, ctx));
    } else {
      record.setJwks(new JWKSet(this.registry.getDefaultKey()));
    }

    return record;
  }
}
