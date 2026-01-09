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
package se.swedenconnect.oidf.resolver;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of multiple caches.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ResolverCacheRegistry {
  private final Map<String, ResolverCacheRegistration> registrations = new ConcurrentHashMap<>();

  /**
   * @param entityId     of the cache to register
   * @param registration for the cache
   */
  public void registerCache(final String entityId, final ResolverCacheRegistration registration) {
    this.registrations.put(entityId, registration);
  }

  /**
   * Updates the version for a given entityId.
   *
   * @param entityId to update
   */
  public void updateVersion(final String entityId) {
    this.getRegistration(entityId).ifPresent(c -> c.cache().useNextVersion());
  }

  /**
   * @param entityId of registration
   * @return registration if present
   */
  public Optional<ResolverCacheRegistration> getRegistration(final String entityId) {
    final Optional<ResolverCacheRegistration> cacheRegistration = Optional.ofNullable(this.registrations.get(entityId));
    if (cacheRegistration.isEmpty()) {
      log.warn("Tried to access cache by entityId {} but no such cache exists", entityId);
    }
    return cacheRegistration;
  }
}
