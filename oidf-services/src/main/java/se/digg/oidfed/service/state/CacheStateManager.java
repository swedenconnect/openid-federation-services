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
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;
import se.digg.oidfed.service.submodule.RequestResponseEntry;

import java.util.Optional;
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
  private final ResolverCacheRegistry registry;
  private final CompositeRecordSource source;
  private final ServiceLock serviceLock;

  /**
   * Constructor.
   * @param registry to query
   * @param source to query for modules
   * @param serviceLock for locking
   */
  public CacheStateManager(
      final ResolverCacheRegistry registry,
      final CompositeRecordSource source,
      final ServiceLock serviceLock) {

    this.registry = registry;
    this.source = source;
    this.serviceLock = serviceLock;
  }

  /**
   * Reloads cached requests if needed.
   */
  @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
  public void reloadCache() {
    if (this.serviceLock.acquireLock(NAME)) {
      this.source.getResolverProperties().stream()
          .map(prop -> this.registry.getModuleCache(prop.entityIdentifier()))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(cache -> {
            cache.flushRequestKeys().forEach(
                key -> {
                  log.debug("Re-calculating cache for {}", key);
                  Optional.ofNullable(cache.resolve(key))
                      .ifPresent(resolve -> {
                        cache.add(new RequestResponseEntry(key, resolve));
                      });
                }
            );
          });
    }
    this.serviceLock.close(NAME);
  }
}
