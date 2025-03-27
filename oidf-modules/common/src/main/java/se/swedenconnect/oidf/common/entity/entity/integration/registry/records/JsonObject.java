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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Json Object helper class for managing serialization.
 *
 * @author Felix Hellman
 */
public class JsonObject {
  private final Map<String, Object> json;

  /**
   * Constructor.
   * @param json to handle
   */
  public JsonObject(final Map<String, Object> json) {
    this.json = json;
  }

  /**
   * Get value as string
   * @param key
   * @return string value
   */
  public String getStringValue(final String key) {
    return (String) this.json.get(key);
  }

  /**
   * @param key
   * @return object list claim
   */
  public List<Object> getObjectListClaim(final String key) {
    return Optional.ofNullable((List<Object>) this.json.get(key))
        .orElse(List.of());
  }
}
