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
package se.swedenconnect.oidf.configuration;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.TypeAdapterFactory;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.jose.shaded.gson.stream.JsonReader;
import com.nimbusds.jose.shaded.gson.stream.JsonWriter;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * GSON type adapter factory for composite records.
 *
 * <p>
 * The record is written in streaming mode, not as a tree, so that adapters further down (see
 * {@code PolicyRecordTypeAdapterFactory}) can write JSON null values that must be kept.
 * </p>
 *
 * @author Felix Hellman
 */
public class CompositeRecordSerializer implements TypeAdapterFactory {

  private static final Type ENTITIES_TYPE = new TypeToken<ArrayList<EntityRecord>>() {
  }.getType();

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    if (!CompositeRecord.class.equals(type.getRawType())) {
      return null;
    }
    return (TypeAdapter<T>) new CompositeRecordTypeAdapter(gson);
  }

  /**
   * Type adapter for composite records.
   *
   * @author Felix Hellman
   */
  private static class CompositeRecordTypeAdapter extends TypeAdapter<CompositeRecord> {

    private final Gson gson;

    /**
     * Constructor.
     *
     * @param gson for the parts of the record
     */
    CompositeRecordTypeAdapter(final Gson gson) {
      this.gson = gson;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final JsonWriter out, final CompositeRecord compositeRecord) throws IOException {
      if (compositeRecord == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      out.name("expiration");
      this.gson.getAdapter(Instant.class).write(out, compositeRecord.getExpiration());
      out.name("issuedAt");
      this.gson.getAdapter(Instant.class).write(out, compositeRecord.getIssuedAt());
      out.name("entities");
      this.gson.getAdapter(new TypeToken<List<EntityRecord>>() {
      }).write(out, compositeRecord.getEntityRecords().getValue());
      out.name("module");
      this.gson.getAdapter(ModuleRecord.class).write(out, compositeRecord.getModuleRecord().getValue());
      out.endObject();
    }

    /** {@inheritDoc} */
    @Override
    public CompositeRecord read(final JsonReader in) throws IOException {
      final JsonElement jsonElement = JsonParser.parseReader(in);
      if (jsonElement.isJsonNull()) {
        return null;
      }
      final JsonObject json = jsonElement.getAsJsonObject();
      final Instant expiration = this.gson.fromJson(json.get("expiration"), Instant.class);
      final Instant issuedAt = this.gson.fromJson(json.get("issuedAt"), Instant.class);
      final List<EntityRecord> entityRecords = this.gson.fromJson(json.get("entities"), ENTITIES_TYPE);
      final ModuleRecord moduleRecord = this.gson.fromJson(json.get("module"), ModuleRecord.class);

      return new CompositeRecord(
          new Expirable<>(expiration, issuedAt, entityRecords),
          new Expirable<>(expiration, issuedAt, moduleRecord)
      );
    }
  }
}
