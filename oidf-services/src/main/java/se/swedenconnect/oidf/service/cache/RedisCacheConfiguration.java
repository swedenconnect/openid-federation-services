/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.ResolverResponseCache;
import se.swedenconnect.oidf.common.entity.entity.integration.SubordinateFetchCache;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkCache;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkStatusCache;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import se.swedenconnect.oidf.CacheFactory;
import se.swedenconnect.oidf.FederationServiceState;
import se.swedenconnect.oidf.resolver.ResolverCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheRepository;
import se.swedenconnect.oidf.service.configuration.FederationServiceProperties;
import se.swedenconnect.oidf.service.resolver.ResolverCacheTransformer;
import se.swedenconnect.oidf.service.resolver.cache.RedisResolverCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverRedisOperations;
import se.swedenconnect.oidf.service.state.RedisFederationServiceState;
import se.swedenconnect.oidf.service.state.RedisServiceLock;
import se.swedenconnect.oidf.service.state.ServiceLock;
import se.swedenconnect.oidf.service.submodule.RequestResponseEntry;

import java.time.Clock;

/**
 * Configuration class for redis.
 *
 * @author Felix Hellman
 */
@Configuration
@ConditionalOnProperty(name = "federation.service.storage", havingValue = "redis")
@Import(DataRedisAutoConfiguration.class)
public class RedisCacheConfiguration {

  @Bean
  CacheFactory redisCacheFactory(final RedisConnectionFactory factory, final Clock clock,
                                 final FederationServiceProperties properties, final Gson gson) {
    return new RedisCacheFactory(clock, factory, properties.getRedis().getKeyName(), gson);
  }

  @Bean
  ResolverRedisOperations redisOperations(
      final RedisTemplate<String, ScrapedEntity> template,
      final RedisConnectionFactory connectionFactory,
      final InstanceSpecificRedisKeySerializer keySerializer
  ) {
    final RedisTemplate<String, String> stringTemplate = new RedisTemplate<>();
    stringTemplate.setConnectionFactory(connectionFactory);
    stringTemplate.setKeySerializer(keySerializer);
    stringTemplate.afterPropertiesSet();
    return new ResolverRedisOperations(template, stringTemplate);
  }

  @Bean
  RedisTemplate<String, ScrapedEntity> entityStatementRedisTemplate(
      final RedisConnectionFactory factory,
      final InstanceSpecificRedisKeySerializer keySerializer) {
    final RedisTemplate<String, ScrapedEntity> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setValueSerializer(new ResolverEntitySerializer());
    template.setKeySerializer(keySerializer);
    template.afterPropertiesSet();
    return template;
  }

  @Bean
  RedisTemplate<String, Long> longRedisTemplate(
      final RedisConnectionFactory factory,
      final InstanceSpecificRedisKeySerializer keySerializer
  ) {
    final RedisTemplate<String, Long> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    return template;
  }

  @Bean
  RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate(
      final RedisConnectionFactory factory,
      final InstanceSpecificRedisKeySerializer keySerializer
  ) {
    final RedisTemplate<String, RequestResponseEntry> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    return template;
  }

  @Bean
  ResolverCacheFactory resolverCacheFactory(
      final RedisTemplate<String, Long> versionTemplate,
      final ResolverRedisOperations resolverRedisOperations
  ) {
    return new RedisResolverCacheFactory(
        versionTemplate,
        resolverRedisOperations
    );
  }

  @Bean
  ResolverCacheTransformer resolverCacheTransformer(final ManagedCacheRepository repository) {

    return new ResolverCacheTransformer(repository);
  }

  @Bean
  FederationServiceState redisFederationServiceState(
      final RedisConnectionFactory factory,
      final InstanceSpecificRedisKeySerializer keySerializer) {

    final RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    template.afterPropertiesSet();
    return new RedisFederationServiceState(template);
  }

  @Bean
  InstanceSpecificRedisKeySerializer instanceSpecificRedisKeySerializer(
      final FederationServiceProperties properties) {
    return new InstanceSpecificRedisKeySerializer(
        new StringRedisSerializer(),
        properties.getRedis().getKeyName()
    );
  }

  @Bean
  ServiceLock redisServiceLock(
      final RedisConnectionFactory factory,
      final InstanceSpecificRedisKeySerializer keySerializer) {


    final RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    template.afterPropertiesSet();
    return new RedisServiceLock(template);
  }

  @Bean
  TrustMarkStatusCache trustMarkStatusCache(final RedisTemplate<String, String> template) {
    return new RedisTrustMarkStatusCache(template);
  }

  @Bean
  ResolverResponseCache redisResolverResponseCache(final RedisTemplate<String, String> template) {
    return new RedisResolverResponseCache(template);
  }

  @Bean
  SubordinateFetchCache redisSubordinateFetchCache(final RedisTemplate<String, String> template) {
    return new RedisSubordinateFetchCache(template);
  }

  @Bean
  TrustMarkCache redisTrustMarkCache(final RedisTemplate<String, String> template) {
    return new RedisTrustMarkCache(template);
  }
}
