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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.service.cache.managed.NoopRequestResponseCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheFactory;
import se.swedenconnect.oidf.service.cache.managed.ManagedCacheRepository;
import se.swedenconnect.oidf.service.cache.managed.RequestResponseCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.InMemoryResolverCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheFactory;
import se.swedenconnect.oidf.service.state.FederationServiceState;
import se.swedenconnect.oidf.service.state.InMemoryFederationServiceState;
import se.swedenconnect.oidf.service.state.NoOperationServiceLock;
import se.swedenconnect.oidf.service.state.ServiceLock;

import java.time.Clock;

/**
 * Configuration class for cache.
 *
 * @author Felix Hellman
 */
@Configuration
@ConditionalOnProperty(name = "openid.federation.storage", havingValue = "memory")
public class InMemoryCacheConfiguration {

  @Bean
  CacheFactory inMemoryCacheFactory(final Clock clock) {
    return new InMemoryCacheFactory(clock);
  }

  @Bean
  ResolverCacheFactory inMemoryResolverCacheFactory() {
    return new InMemoryResolverCacheFactory();
  }

  @Bean
  FederationServiceState inMemoryFederationServiceState() {
    return new InMemoryFederationServiceState();
  }

  @Bean
  ServiceLock noOperationServiceLock() {
    return new NoOperationServiceLock();
  }

  @Bean
  ManagedCacheRepository managedCacheRepository(final ManagedCacheFactory factory) {
    return new ManagedCacheRepository(factory);
  }

  @Bean
  ManagedCacheFactory managedNoOpCacheFactory(final RequestResponseCacheFactory factory) {
    return new ManagedCacheFactory(factory);
  }

  @Bean
  RequestResponseCacheFactory noOpRequestResponseCacheFactory() {
    return new NoopRequestResponseCacheFactory();
  }
}
