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
package se.swedenconnect.oidf.configuration;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.CacheFactory;
import se.swedenconnect.oidf.FederationKeys;
import se.swedenconnect.oidf.InMemoryCacheFactory;
import se.swedenconnect.oidf.OpenIdFederationProperties;
import se.swedenconnect.oidf.RestClientFactory;
import se.swedenconnect.oidf.RestClientRecordIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.Cache;
import se.swedenconnect.oidf.common.entity.entity.integration.CacheRecordPopulator;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.JWSRegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Registry Configuration.
 *
 * @author Felix Hellman
 */
@Configuration
public class FederationRegistryConfiguration {
  @Bean
  @ConditionalOnMissingBean(CacheFactory.class)
  CacheFactory inMemoryCacheFactory(final Clock clock) {
    return new InMemoryCacheFactory(clock);
  }

  @Bean
  Cache<String, CompositeRecord> compositeRecordCache(final CacheFactory cacheFactory) {
    return cacheFactory.create(CompositeRecord.class);
  }

  @Bean
  CachedRecordSource cachedRecordSource(final Cache<String, CompositeRecord> cache) {
    return new CachedRecordSource(cache);
  }

  @Bean
  RegistryVerifier registryVerifier(final FederationKeys federationKeys) {
    return new JWSRegistryVerifier(federationKeys.validationKeys());
  }

  @Bean
  RestClientFactory restClientFactory(final SslBundles sslBundle, final ObservationRegistry registry) {
    return new RestClientFactory(sslBundle, registry);
  }

  @Bean
  RestClient registryRestClient(final RestClientFactory restClientFactory,
                                final OpenIdFederationProperties properties) {
    return restClientFactory.create(properties.getRegistry().getIntegration().getClient());
  }

  @Bean
  RecordRegistryIntegration recordRegistryIntegration(
      final RegistryVerifier verifier,
      @Qualifier("registryRestClient") final RestClient restClient) {

    return new RestClientRecordIntegration(verifier, restClient);
  }

  @Bean
  CacheRecordPopulator cacheRecordPopulator(
      final CachedRecordSource source,
      final RecordRegistryIntegration integration,
      final OpenIdFederationProperties properties
  ) {
    return new CacheRecordPopulator(source, integration, properties.getInstanceId());
  }

  @Bean
  Clock systemClock() {
    return Clock.system(ZoneId.systemDefault());
  }
}
