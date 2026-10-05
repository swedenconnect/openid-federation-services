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

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses and loads json from registry.
 *
 * @author Felix Hellman
 */
@Slf4j
public class JsonRegistryLoader {
  private final Gson gson;

  /**
   * Constructor.
   * @param gson for serialization
   */
  public JsonRegistryLoader(final Gson gson) {
    this.gson = gson;
  }

  /**
   * Parses entity records from json. An entity that cannot be loaded, for example because a key reference names a
   * key that does not exist, is left out and logged.
   *
   * @param json the entity records
   * @return list of entities
   */
  public List<EntityRecord> parseEntityRecord(final String json) {
    final List<EntityRecord> entities = new ArrayList<>();
    for (final JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
      try {
        final EntityRecord entity = this.gson.fromJson(element, EntityRecord.class);
        if (Objects.isNull(entity.getJwks())) {
          log.error("Ignoring entity {} from registry: no keys available", entityId(element));
          continue;
        }
        entities.add(entity);
      }
      catch (final RuntimeException e) {
        log.error("Ignoring entity {} from registry: {}", entityId(element), e.getMessage());
      }
    }
    return entities;
  }

  /**
   * Gets the entity identifier of an entity record, for logging.
   *
   * @param element the entity record
   * @return the entity identifier, or "unknown" if there is none
   */
  private static String entityId(final JsonElement element) {
    if (element.isJsonObject()) {
      final JsonElement id = ((JsonObject) element).get("entity-identifier");
      if (id != null && id.isJsonPrimitive()) {
        return id.getAsString();
      }
    }
    return "unknown";
  }

  /**
   * Parse module record from json
   * @param json
   * @return module record
   */
  public ModuleRecord parseModuleJson(final String json) {
    try {
      final TypeAdapter<ModuleRecord> adapter = this.gson.getAdapter(new TypeToken<>() {
      });
      return adapter.fromJson(json);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * @param moduleRecord
   * @return json string
   */
  public String toJson(final ModuleRecord moduleRecord) {
    return this.gson.getAdapter(ModuleRecord.class).toJson(moduleRecord);
  }

  /**
   *
   * @param entityRecords
   * @return json string
   */
  public String toJson(final List<EntityRecord> entityRecords) {
    return this.gson.getAdapter(new TypeToken<List<EntityRecord>>(){}).toJson(entityRecords);
  }
}

