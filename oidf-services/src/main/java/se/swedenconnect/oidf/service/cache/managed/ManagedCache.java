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
package se.swedenconnect.oidf.service.cache.managed;

import java.util.Set;

/**
 * Interface for a cache that can be reloaded externally.
 * @param <K> key type
 * @param <V> value type
 *
 * @author Felix Hellman
 */
public interface ManagedCache<K,V> {
  /**
   * @param key to add
   * @param value to add
   */
  void add(final K key, final V value);

  /**
   * @param key for value
   * @return value or null
   */
  V get(final K key);

  /**
   * @param key to fetch
   * @return new value from source
   */
  V fetch(final K key);

  /**
   * Removes all keys and returns a subset depending on configuration.
   * @return set of requests that has a score higher than configured threshold
   */
  Set<K> flushRequestKeys();
}
