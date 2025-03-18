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
package se.digg.oidfed.service.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.service.cache.managed.ManagedCache;
import se.digg.oidfed.service.cache.managed.ManagedCacheRepository;
import se.digg.oidfed.service.cache.managed.ModuleLoader;

import java.util.concurrent.TimeUnit;

/**
 * Keeps track of cached request/responses.
 *
 * @author Felix Helman
 */
@Component
@Slf4j
public class CacheStateManager {
  /**
   * Name of this component.
   */
  public static final String NAME = "cache-state-manager";
  private final ManagedCacheRepository repository;
  private final ServiceLock serviceLock;
  private final ModuleLoader loader;

  /**
   * Constructor.
   *
   * @param repository  to query
   * @param serviceLock for locking
   * @param loader      for loading modules
   */
  public CacheStateManager(
      final ManagedCacheRepository repository,
      final ServiceLock serviceLock,
      final ModuleLoader loader) {

    this.repository = repository;
    this.serviceLock = serviceLock;
    this.loader = loader;
  }

  /**
   * Reloads cached requests if needed.
   */
  @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
  public void reloadCache() {
    if (this.serviceLock.acquireLock(NAME)) {
      this.loader.getResolvers().forEach(resolver -> {
        final ManagedCache<String, String> cache = this.repository.createIfMissing(resolver);
        cache.flushRequestKeys()
            .forEach(key -> {
              try {
                final String response = resolver.resolve(ResolveRequest.fromKey(key));
                cache.add(key, response);
              } catch (final FederationException e) {
                log.warn("Failed to refresh key {} for resolver {}, dropping key.", key,
                    resolver.getEntityId().getValue());
              }
            });
      });
    }
    this.serviceLock.close(NAME);
  }
}
