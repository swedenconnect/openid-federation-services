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
package se.digg.oidfed.service.cache.managed;

import se.digg.oidfed.resolver.Resolver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gets or lazily creates a cache for a given entity that needs to be managed externally.
 *
 * @author Felix Hellman
 */
public class ManagedCacheRepository {
  private final ManagedCacheFactory factory;
  private final Map<String, ManagedCache<String, String>> resolverCaches = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param factory
   */
  public ManagedCacheRepository(final ManagedCacheFactory factory) {
    this.factory = factory;
  }

  /**
   * @param resolver to get cache for
   * @return cache
   */
  public ManagedCache<String, String> createIfMissing(final Resolver resolver) {
    return Optional.ofNullable(this.resolverCaches.get(resolver.getEntityId().getValue()))
        .orElseGet(() -> {
          final ManagedCache<String, String> resolverCache = this.factory.resolverCache(resolver);
          this.resolverCaches.put(resolver.getEntityId().getValue(), resolverCache);
          return resolverCache;
        });
  }
}
