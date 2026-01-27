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

import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;

/**
 * Loader for entity record by reference
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class EntityRecordPropertyLoader implements Converter<String, List<EntityRecord>> {
  private final JsonReferenceLoader jsonReferenceLoader;
  @Nullable
  @Override
  public List<EntityRecord> convert(final String source) {
    final List<EntityRecord> entityRecords = this.jsonReferenceLoader.loadJson(source, new TypeToken<>() {
    });
    return entityRecords;
  }
}
