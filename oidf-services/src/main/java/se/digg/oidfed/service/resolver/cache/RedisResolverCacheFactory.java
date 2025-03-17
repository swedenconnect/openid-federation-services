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
import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.entity.integration.properties.ResolverProperties;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.submodule.RedisModuleRequestResponseCache;
import se.digg.oidfed.service.submodule.RequestResponseEntry;

/**
 * Redis implementation for Resolver Cache Factory.
 *
 * @author Felix Hellman
 */
@Slf4j
public class RedisResolverCacheFactory implements ResolverCacheFactory {

  private final RedisTemplate<String, Integer> versionTemplate;
  private final RedisTemplate<String, String> requestSetTemplate;
  private final RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate;
  private final ResolverRedisOperations resolverRedisOperations;

  /**
   * Constructor.
   * @param versionTemplate for keeping track of versions
   * @param requestSetTemplate for listing request popularity
   * @param requestResponseEntryRedisTemplate for requests
   * @param resolverRedisOperations for performing operations
   */
  public RedisResolverCacheFactory(
      final RedisTemplate<String, Integer> versionTemplate,
      final RedisTemplate<String, String> requestSetTemplate,
      final RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate,
      final ResolverRedisOperations resolverRedisOperations) {

    this.versionTemplate = versionTemplate;
    this.requestSetTemplate = requestSetTemplate;
    this.requestResponseEntryRedisTemplate = requestResponseEntryRedisTemplate;
    this.resolverRedisOperations = resolverRedisOperations;
  }

  @Override
  public ResolverCache create(final ResolverProperties properties) {
    return new RedisVersionedCacheLayer(this.versionTemplate, this.resolverRedisOperations, properties);
  }

  @Override
  public RedisModuleRequestResponseCache createModuleCache(final Resolver resolver) {
    return new RedisModuleRequestResponseCache(
        this.requestResponseEntryRedisTemplate,
        this.requestSetTemplate,
        key -> {
          try {
            return resolver.resolve(ResolveRequest.fromKey(key));
          } catch (final FederationException e) {
            log.warn("Failed to refresh resolve request {}, continuing ...", key);
            return null;
          }
        }
    );
  }
}
