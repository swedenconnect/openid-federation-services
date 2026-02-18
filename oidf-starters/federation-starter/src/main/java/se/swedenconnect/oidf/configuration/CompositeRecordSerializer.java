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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GSON serializer for composite records.
 *
 * @author Felix Hellman
 */
public class CompositeRecordSerializer implements JsonSerializer<CompositeRecord>, JsonDeserializer<CompositeRecord> {

  @Override
  public CompositeRecord deserialize(
      final JsonElement jsonElement,
      final Type type,
      final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    final Instant expiration = jsonDeserializationContext
        .deserialize(jsonElement.getAsJsonObject().get("expiration"), Instant.class);
    final Instant issuedAt = jsonDeserializationContext
        .deserialize(jsonElement.getAsJsonObject().get("issuedAt"), Instant.class);

    final Type entityType = new TypeToken<ArrayList<EntityRecord>>() {
    }.getType();
    final Type moduleType = new TypeToken<ModuleRecord>() {
    }.getType();
    final List<EntityRecord> entityRecords = jsonDeserializationContext
        .deserialize(jsonElement.getAsJsonObject().get("entities"), entityType);
    final ModuleRecord moduleRecord = jsonDeserializationContext
        .deserialize(jsonElement.getAsJsonObject().get("module"), moduleType);

    return new CompositeRecord(
        new Expirable<>(expiration, issuedAt, entityRecords),
        new Expirable<>(expiration, issuedAt, moduleRecord)
    );
  }

  @Override
  public JsonElement serialize(
      final CompositeRecord compositeRecord,
      final Type type,
      final JsonSerializationContext jsonSerializationContext) {

    final JsonObject json = new JsonObject();

    final Instant expiration = compositeRecord.getExpiration();
    final Instant issuedAt = compositeRecord.getIssuedAt();
    json.add("expiration", jsonSerializationContext.serialize(expiration));
    json.add("issuedAt", jsonSerializationContext.serialize(issuedAt));
    final ModuleRecord moduleRecord = compositeRecord.getModuleRecord().getValue();
    final List<EntityRecord> entityRecords = compositeRecord.getEntityRecords().getValue();
    json.add("entities", jsonSerializationContext.serialize(entityRecords));
    json.add("module", jsonSerializationContext.serialize(moduleRecord));
    return json;
  }
}
