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

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.io.IOException;
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
   * Parse EntityRecords from json
   * @param json
   * @return list of entities
   */
  public List<EntityRecord> parseEntityRecord(final String json) {
    try {
      final TypeAdapter<List<EntityRecord>> adapter = this.gson.getAdapter(new TypeToken<>() {
      });
      return adapter.fromJson(json).stream()
          .filter(er -> {
            final boolean jwkIsMissingFromEntity = Objects.isNull(er.getJwks());
            if (jwkIsMissingFromEntity) {
              log.error("Failed to load entity {} due to no JWK available, is default key missing?",
                  er.getEntityIdentifier().getValue());
            }
            return !jwkIsMissingFromEntity;
          })
          .toList();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
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

