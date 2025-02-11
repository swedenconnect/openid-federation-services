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
package se.digg.oidfed.service.cache;

import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.ListCache;

import java.io.Serializable;
import java.util.List;

/**
 * Factory class for creating caches.
 *
 * @author Felix Hellman
 */
public interface CacheFactory {
  /**
   * Creates a single value cache.
   *
   * @param v   value class
   * @param <K> key class
   * @param <V> value class
   * @return cache for given key/value
   */
  <K extends Serializable, V> Cache<K, V> create(final Class<V> v);

  /**
   * Creates a single value cache. (where the value is a list)
   *
   * @param v value class
   * @param <K> key class
   * @param <V> value class
   * @return cache for list value
   */
  <K extends Serializable, V> Cache<K, List<V>> createListValueCache(final Class<V> v);

  /**
   * Create list cache
   * @param v value class
   * @param <K> key class
   * @param <V> value class
   * @return list cache with list operations
   */
  <K extends Serializable, V> ListCache<K, V> createListCache(final Class<V> v);
}
