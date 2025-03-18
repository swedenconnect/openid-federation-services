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
package se.swedenconnect.oidf.service.resolver;

import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheRepository;
import se.swedenconnect.oidf.service.submodule.CacheBackedResolver;

import java.util.function.Function;

/**
 * Transformer function that takes a resolver and turns it into a cached resolver.
 *
 * @author Felix Hellman
 */
public class ResolverCacheTransformer implements Function<Resolver, Resolver> {

  private final ManagedCacheRepository repository;

  /**
   * Constructor.
   *
   * @param repository
   */
  public ResolverCacheTransformer(final ManagedCacheRepository repository) {
    this.repository = repository;
  }

  @Override
  public Resolver apply(final Resolver resolver) {
    return new CacheBackedResolver(resolver, this.repository.createIfMissing(resolver));
  }
}
