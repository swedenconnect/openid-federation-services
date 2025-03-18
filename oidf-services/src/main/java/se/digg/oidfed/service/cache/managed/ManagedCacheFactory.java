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

import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.submodule.RequestResponseEntry;
import se.digg.oidfed.service.submodule.RequestResponseModuleCache;

import java.util.Optional;
import java.util.Set;

/**
 * Creates and a relevant cache that needs to be updated by a scheduled job.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ManagedCacheFactory {

  private final RequestResponseCacheFactory factory;


  /**
   * Constructor.
   * @param factory
   */
  public ManagedCacheFactory(final RequestResponseCacheFactory factory) {
    this.factory = factory;
  }

  /**
   * Creates resolver cache.
   * @param resolver to create cache for
   * @return cache
   */
  public ManagedCache<String, String> resolverCache(final Resolver resolver) {
    final RequestResponseModuleCache requestResponseModuleCache = this.factory.create(resolver.getEntityId());

    return new ManagedCache<>() {
      @Override
      public void add(final String key, final String value) {
        requestResponseModuleCache.add(new RequestResponseEntry(key, value));
      }

      @Override
      public String get(final String key) {
        return Optional.ofNullable(requestResponseModuleCache.get(key))
            .map(RequestResponseEntry::getResponse)
            .orElse(null);
      }

      @Override
      public String fetch(final String key) {
        try {
          return resolver.resolve(ResolveRequest.fromKey(key));
        } catch (final FederationException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Set<String> flushRequestKeys() {
        return requestResponseModuleCache.flushRequestKeys();
      }
    };
  }
}
