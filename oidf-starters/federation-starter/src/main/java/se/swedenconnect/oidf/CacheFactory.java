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
package se.swedenconnect.oidf;

import se.swedenconnect.oidf.common.entity.entity.integration.Cache;

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
   * @param <V> value class
   * @return cache for given key/value
   */
  <V> Cache<String, V> create(final Class<V> v);
}
