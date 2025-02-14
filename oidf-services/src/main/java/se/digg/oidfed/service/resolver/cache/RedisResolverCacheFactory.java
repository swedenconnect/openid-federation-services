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

import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.tree.ResolverCache;

/**
 * Redis implementation for Resolver Cache Factory.
 *
 * @author Felix Hellman
 */
public class RedisResolverCacheFactory implements ResolverCacheFactory {

  private final RedisTemplate<String, Integer> versionTemplate;
  private final ResolverRedisOperations resolverRedisOperations;

  /**
   * Constructor.
   * @param versionTemplate for keeping track of versions
   * @param resolverRedisOperations for performing operations
   */
  public RedisResolverCacheFactory(
      final RedisTemplate<String, Integer> versionTemplate,
      final ResolverRedisOperations resolverRedisOperations) {

    this.versionTemplate = versionTemplate;
    this.resolverRedisOperations = resolverRedisOperations;
  }

  @Override
  public ResolverCache create(final ResolverProperties properties) {
    return new RedisVersionedCacheLayer(this.versionTemplate, this.resolverRedisOperations, properties);
  }
}
