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

/**
 * List cache is a cache with append and getAll operation.
 * Useful for representing lists.
 * @param <K> key
 * @param <V> value
 *
 * @author Felix Hellman
 */
public interface ListCache<K,V> {
  /**
   * Appens value to a list (by key)
   * @param key list to add to
   * @param value to add
   */
  void append(final K key, final V value);

  /**
   * @param key of a list
   * @return all values
   */
  List<V> getAll(final K key);
}
