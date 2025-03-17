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
package se.digg.oidfed.service.resolver;

import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.resolver.cache.ResolverCacheFactory;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;
import se.digg.oidfed.service.submodule.CacheBackedResolver;
import se.digg.oidfed.service.submodule.ResolverRequestResponseModuleCache;

import java.util.Optional;
import java.util.function.Function;

/**
 * Transformer function that takes a resolver and turns it into a cached resolver.
 *
 * @author Felix Hellman
 */
public class ResolverCacheTransformer implements Function<Resolver, Resolver> {

  private final ResolverCacheRegistry registry;
  private final ResolverCacheFactory factory;

  /**
   * Constructor.
   * @param registry
   * @param factory
   */
  public ResolverCacheTransformer(
      final ResolverCacheRegistry registry,
      final ResolverCacheFactory factory) {

    this.registry = registry;
    this.factory = factory;
  }

  @Override
  public Resolver apply(final Resolver resolver) {
    final Optional<ResolverRequestResponseModuleCache> moduleCache =
        this.registry.getModuleCache(resolver.getEntityId().getValue());
    if (moduleCache.isEmpty()) {
      final ResolverRequestResponseModuleCache cache = this.factory.createModuleCache(resolver);
      this.registry.registerModuleCache(
          resolver.getEntityId().getValue(),
          cache
      );
      return new CacheBackedResolver(resolver, cache);
    }
    return new CacheBackedResolver(
        resolver,
        this.registry.getModuleCache(resolver.getEntityId().getValue()).get()
    );
  }
}
