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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cache with support for additional search keys.
 * @param <V> value to store
 *
 * @author Felix Hellman
 */
public interface MultiKeyCache<V> {
  /**
   * @param primaryKey for entity
   * @param subKeys for entity
   * @param value of the entity
   */
  void add(final String primaryKey, final Map<String,String> subKeys, final V value);

  /**
   * @param primaryKey for entity
   * @param value of the entity
   */
  void add(final String primaryKey, final V value);

  /**
   * @param request for cache
   * @return value
   */
  V get(final CacheRequest request);

  Set<String> getPrimaryKeys();

  Set<String> getSubKeys(final String subKeyName);
}
