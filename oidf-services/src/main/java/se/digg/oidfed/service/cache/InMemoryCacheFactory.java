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
import se.digg.oidfed.common.entity.integration.InMemoryCache;

import java.time.Clock;
import java.util.List;

/**
 * Factory for creating in memory caches.
 *
 * @author Felix Hellman
 */
public class InMemoryCacheFactory implements CacheFactory {

  private final Clock clock;

  /**
   * Constructor.
   *
   * @param clock for time keeping.
   */
  public InMemoryCacheFactory(final Clock clock) {
    this.clock = clock;
  }

  @Override
  public <V> Cache<String, V> create(final Class<V> v) {
    return new InMemoryCache<>(this.clock);
  }

  @Override
  public <V> Cache<String, List<V>> createListValueCache(final Class<V> v) {
    return new InMemoryCache<>(this.clock);
  }
}
