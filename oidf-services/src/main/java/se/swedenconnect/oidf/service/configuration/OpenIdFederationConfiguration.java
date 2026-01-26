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
package se.swedenconnect.oidf.service.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.FederationProperties;
import se.swedenconnect.oidf.FederationServiceState;
import se.swedenconnect.oidf.InMemoryFederationServiceState;
import se.swedenconnect.oidf.common.entity.entity.integration.CacheRecordPopulator;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.resolver.ResolverCacheRegistry;
import se.swedenconnect.oidf.resolver.ResolverFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheRepository;
import se.swedenconnect.oidf.service.cache.managed.NoopRequestResponseCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.RequestResponseCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.CompositeTreeLoader;
import se.swedenconnect.oidf.service.state.NoOperationServiceLock;
import se.swedenconnect.oidf.service.state.RegistryStateManager;
import se.swedenconnect.oidf.service.state.ServiceLock;

/**
 * Configuration class for openid federation.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(FederationServiceProperties.class)
public class OpenIdFederationConfiguration {

  @Bean
  @ConditionalOnMissingBean
  ServiceLock noOperationServiceLock() {
    return new NoOperationServiceLock();
  }

  @Bean
  @ConditionalOnMissingBean
  FederationServiceState inMemoryFederationState() {
    return new InMemoryFederationServiceState();
  }

  @Bean
  RegistryStateManager registryStateManager(
      final CacheRecordPopulator populator,
      final FederationServiceState state,
      final ServiceLock lock,
      final ApplicationEventPublisher publisher,
      final FederationProperties properties
  ) {
    return new RegistryStateManager(populator,
        state,
        lock,
        publisher,
        properties
    );
  }

  @Bean
  CompositeTreeLoader compositeTreeLoader(final ResolverCacheRegistry resolverCacheRegistry,
                                          final ResolverFactory resolverFactory,
                                          final CompositeRecordSource recordSource) {
    return new CompositeTreeLoader(resolverCacheRegistry, resolverFactory, recordSource);
  }

  @Bean
  RequestResponseCacheFactory redisRequestResponseCacheFactory() {
    return new NoopRequestResponseCacheFactory();
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
