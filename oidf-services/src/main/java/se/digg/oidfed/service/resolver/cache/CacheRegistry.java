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
package se.digg.oidfed.service.resolver.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of multiple caches.
 *
 * @author Felix Hellman
 */
@Slf4j
public class CacheRegistry {
  private final Map<String, CacheRegistration> registrations = new ConcurrentHashMap<>();

  /**
   * @param alias of the cache to register
   * @param registration for the cache
   */
  public void registerCache(final String alias, final CacheRegistration registration) {
    this.registrations.put(alias, registration);
  }

  /**
   * @return a set of all aliases
   */
  public Set<String> getAliases() {
    return this.registrations.keySet();
  }

  /**
   * Updates the version for a given alias.
   * @param alias to update
   */
  public void updateVersion(final String alias) {
    this.getRegistration(alias).ifPresent(c -> c.cache().useNextVersion());
  }

  /**
   * Load/Reloads a tree.
   * @param alias to load
   */
  public void loadTree(final String alias) {
    this.getRegistration(alias).ifPresent(r -> {
      r.tree()
          .load(r.loader(), "%s/.well-known/openid-federation".formatted(r.properties().trustAnchor()));
    });
  }

  private Optional<CacheRegistration> getRegistration(final String alias) {
    final Optional<CacheRegistration> cacheRegistration = Optional.ofNullable(this.registrations.get(alias));
    if (cacheRegistration.isEmpty()) {
      log.warn("Tried to access cache by alias {} but no such cache exists", alias);
    }
    return cacheRegistration;
  }
}
