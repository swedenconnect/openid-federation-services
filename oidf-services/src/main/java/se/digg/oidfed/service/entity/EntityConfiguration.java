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
package se.digg.oidfed.service.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.DelegatingEntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityConfigurationFactory;
import se.digg.oidfed.common.entity.EntityPathFactory;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityRecordVerifier;
import se.digg.oidfed.common.entity.InMemoryEntityRecordRegistry;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.entity.integration.CachedRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.InMemoryRecordRegistryCache;
import se.digg.oidfed.common.entity.integration.InMemoryTrustMarkCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.entity.integration.TrustMarkIntegration;
import se.digg.oidfed.common.entity.integration.TrustMarkLoadingCache;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.keys.FederationKeys;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
public class EntityConfiguration {

  /**
   * Creates an instance of {@link EntityRecordRegistry} using the provided configuration properties and event
   * publisher.
   *
   * @param properties the configuration properties defining the entity registry setup, including base path and
   *                   entities.
   * @param publisher  the application event publisher used to publish events when entities are registered.
   * @return a configured instance of {@link EntityRecordRegistry} that delegates operations and publishes registration
   * events.
   */
  @Bean
  EntityRecordRegistry entityRegistry(final OpenIdFederationConfigurationProperties properties,
                                      final ApplicationEventPublisher publisher
  ) {
    return new DelegatingEntityRecordRegistry(
        new InMemoryEntityRecordRegistry(
            new EntityPathFactory(properties.getModules().getIssuers())),
        List.of(er -> publisher.publishEvent(new EntityRegisteredEvent(er))));
  }

  /**
   * Factory method to create an instance of {@link EntityConfigurationFactory}.
   *
   * @param factory               for signing
   * @param trustMarkLoadingCache for fetching trust marks
   * @return an instance of {@link EntityConfigurationFactory} configured with the specified signing key
   */
  @Bean
  EntityConfigurationFactory entityStatementFactory(final SignerFactory factory,
                                                    final TrustMarkLoadingCache trustMarkLoadingCache
  ) {
    return new EntityConfigurationFactory(factory, trustMarkLoadingCache);
  }

  /**
   * Creates and configures a TrustMarkSubjectRecordVerifier bean.
   * @param keys federation keys
   *
   * @return a TrustMarkSubjectRecordVerifier instance configured with the keys retrieved from the registry
   * based on the provided properties.
   */
  @Bean
  @Qualifier("entity-record-verifier")
  EntityRecordVerifier entityRecordVerifier(
      final FederationKeys keys) {
    return new EntityRecordVerifier(keys.validationKeys());
  }

  /**
   * Creates and returns a {@link RecordRegistrySource} bean, configured with caching functionality if the
   * "openid.federation.entity-registry.client" property is enabled.
   *
   * @param integration the {@link RecordRegistryIntegration} instance used to fetch records from the registry.
   * @param cache       the {@link RecordRegistryCache} instance used to cache registry records.
   * @return a {@link RecordRegistrySource} instance utilizing the provided integration and cache.
   */
  @Bean
  RecordRegistrySource recordRegistrySource(
      final RecordRegistryIntegration integration,
      final RecordRegistryCache cache) {
    return new CachedRecordRegistrySource(integration, cache);
  }

  /**
   * Creates and returns an empty implementation of the RecordRegistrySource interface.
   * This implementation is used when no RecordRegistrySource bean is defined, providing
   * methods that return empty results or default values.
   *
   * @return an empty RecordRegistrySource implementation with methods returning no data
   */
  @Bean
  @ConditionalOnMissingBean(RecordRegistrySource.class)
  RecordRegistrySource emptyRecordRegistrySource() {
    log.warn("Starting application without a connection to an entity registry");
    return new RecordRegistrySource() {
      private final InMemoryRecordRegistryCache cache = new InMemoryRecordRegistryCache();

      @Override
      public Optional<PolicyRecord> getPolicy(final String id) {
        return this.cache.getPolicy(id).data();
      }

      @Override
      public List<EntityRecord> getEntityRecords(final String issuer) {
        return this.cache.getEntityRecords(issuer).data().orElse(List.of());
      }

      @Override
      public void addPolicy(final PolicyRecord record) {
        this.cache.addPolicy(record);
      }
    };
  }

  /**
   * Creates and returns a bean of type {@link RecordRegistryCache}.
   * The implementation returned is an in-memory cache for record registry entities.
   *
   * @return a new instance of {@link RecordRegistryCache} backed by an in-memory store
   */
  @Bean
  RecordRegistryCache recordRegistryCache() {
    return new InMemoryRecordRegistryCache();
  }

  @Bean
  TrustMarkLoadingCache trustMarkLoadingCache(final TrustMarkIntegration integration) {
    return new TrustMarkLoadingCache(new InMemoryTrustMarkCache(), integration);
  }
}
