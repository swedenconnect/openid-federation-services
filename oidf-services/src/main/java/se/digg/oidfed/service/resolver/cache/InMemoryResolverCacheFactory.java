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

import se.digg.oidfed.common.entity.integration.properties.ResolverProperties;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.common.tree.VersionedInMemoryCache;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.submodule.RedisModuleRequestResponseCache;
import se.digg.oidfed.service.submodule.RequestResponseEntry;
import se.digg.oidfed.service.submodule.RequestResponseModuleCache;
import se.digg.oidfed.service.submodule.ResolverRequestResponseModuleCache;

import java.util.Set;

/**
 * Resolver cache factory for in memory caches.
 *
 * @author Felix Hellman
 */
public class InMemoryResolverCacheFactory implements ResolverCacheFactory {

  @Override
  public ResolverCache create(final ResolverProperties properties) {
    return new VersionedInMemoryCache();
  }

  @Override
  public ResolverRequestResponseModuleCache createModuleCache(final Resolver resolver) {
    return new ResolverRequestResponseModuleCache() {
      @Override
      public String resolve(final String request) {
        return null;
      }

      @Override
      public void add(final RequestResponseEntry requestResponseEntry) {

      }

      @Override
      public Set<String> flushRequestKeys() {
        return Set.of();
      }

      @Override
      public RequestResponseEntry get(final String key) {
        return null;
      }
    };
  }
}
