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
package se.digg.oidfed.common.entity.integration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementation of {@link MultiKeyCache}
 *
 * @param <V> value type
 * @author Felix Hellman
 */
public class InMemoryMultiKeyCache<V> implements MultiKeyCache<V> {

  private final Map<String, V> values = new ConcurrentHashMap<>();
  private final Map<String, Map<String, String>> subToPrimaryKey = new ConcurrentHashMap<>();

  @Override
  public void add(final String primaryKey, final Map<String, String> subKeys, final V value) {
    this.values.put(primaryKey, value);
    subKeys.forEach((k, v) -> {
      final Map<String, String> subKeysByName = Optional.ofNullable(this.subToPrimaryKey.get(k))
          .orElseGet(ConcurrentHashMap::new);
      subKeysByName.put(v, primaryKey);
      this.subToPrimaryKey.put(k, subKeysByName);
    });
  }

  @Override
  public void add(final String primaryKey, final V value) {
    this.add(primaryKey, Map.of(), value);
  }

  @Override
  public V get(final CacheRequest request) {
    if (request.isPrimaryKeySearch()) {
      return this.values.get(request.key());
    }

    return Optional.ofNullable(this.subToPrimaryKey.get(request.keyName()))
        .flatMap(keyMap -> Optional.ofNullable(keyMap.get(request.key())))
        .flatMap(primaryKey -> Optional.ofNullable(this.values.get(primaryKey)))
        .orElseGet(() -> null);
  }

  @Override
  public Set<String> getPrimaryKeys() {
    return this.values.keySet();
  }

  @Override
  public Set<String> getSubKeys(final String subKeyName) {
    return this.subToPrimaryKey.get(subKeyName).keySet();
  }
}
