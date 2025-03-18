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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheRepository;
import se.swedenconnect.oidf.service.cache.managed.RedisRequestResponseCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.RequestResponseCacheFactory;
import se.swedenconnect.oidf.service.configuration.OpenIdFederationConfigurationProperties;
import se.swedenconnect.oidf.service.resolver.ResolverCacheTransformer;
import se.swedenconnect.oidf.service.resolver.cache.RedisResolverCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverRedisOperations;
import se.swedenconnect.oidf.service.state.FederationServiceState;
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
@ConditionalOnProperty(name = "openid.federation.storage", havingValue = "redis")
@Import(RedisAutoConfiguration.class)
public class RedisCacheConfiguration {

  @Bean
  CacheFactory redisCacheFactory(final RedisConnectionFactory factory, final Clock clock,
                                 final OpenIdFederationConfigurationProperties properties) {
    return new RedisCacheFactory(clock, factory, properties.getRegistry().getIntegration().getInstanceId());
  }

  @Bean
  ResolverRedisOperations redisOperations(
      final RedisTemplate<String, EntityStatement> template,
      final RedisTemplate<String, String> childrenTemplate
  ) {
    return new ResolverRedisOperations(template, childrenTemplate);
  }

  @Bean
  RedisTemplate<String, EntityStatement> entityStatementRedisTemplate(final RedisConnectionFactory factory) {
    final RedisTemplate<String, EntityStatement> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setValueSerializer(new EntityStatementSerializer());
    return template;
  }

  @Bean
  RedisTemplate<String, Integer> integerRedisTemplate(final RedisConnectionFactory factory) {
    final RedisTemplate<String, Integer> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    return template;
  }

  @Bean
  RedisTemplate<String, RequestResponseEntry> requestResponseEntryRedisTemplate(final RedisConnectionFactory factory) {
    final RedisTemplate<String, RequestResponseEntry> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    return template;
  }

  @Bean
  ResolverCacheFactory resolverCacheFactory(
      final RedisTemplate<String, Integer> versionTemplate,
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
      final OpenIdFederationConfigurationProperties properties) {

    final InstanceSpecificRedisKeySerializer keySerializer =
        new InstanceSpecificRedisKeySerializer(new StringRedisSerializer(),
            properties.getRegistry().getIntegration().getInstanceId());

    final RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    template.afterPropertiesSet();
    return new RedisFederationServiceState(template);
  }

  @Bean
  ServiceLock redisServiceLock(
      final RedisConnectionFactory factory,
      final OpenIdFederationConfigurationProperties properties) {
    final InstanceSpecificRedisKeySerializer keySerializer = new InstanceSpecificRedisKeySerializer(
        new StringRedisSerializer(),
        properties.getRegistry().getIntegration().getInstanceId()
    );

    final RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(keySerializer);
    template.afterPropertiesSet();
    return new RedisServiceLock(template);
  }

  @Bean
  RequestResponseCacheFactory redisRequestResponseCacheFactory(
      final RedisTemplate<String, String> requestTemplate,
      final RedisTemplate<String, RequestResponseEntry> entryTemplate,
      final OpenIdFederationConfigurationProperties properties) {
    return new RedisRequestResponseCacheFactory(entryTemplate, requestTemplate, properties);
  }

  @Bean
  ManagedCacheFactory managedRedisCacheFactory(final RequestResponseCacheFactory factory) {
    return new ManagedCacheFactory(factory);
  }

  @Bean
  ManagedCacheRepository managedRedisCacheRepository(final ManagedCacheFactory cacheFactory) {
    return new ManagedCacheRepository(cacheFactory);
  }
}
