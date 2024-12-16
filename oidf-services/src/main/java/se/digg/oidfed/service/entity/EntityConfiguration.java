/*
 * Copyright 2024 Sweden Connect
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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.DelegatingEntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.common.entity.EntityStatementFactory;
import se.digg.oidfed.common.entity.InMemoryEntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.InMemoryRecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.CachedRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.rest.RestClientRegistry;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRecordVerifier;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EntityConfigurationProperties.class)
public class EntityConfiguration {

  /**
   * Creates an instance of {@link EntityRecordRegistry} using the provided configuration properties and event publisher.
   *
   * @param properties the configuration properties defining the entity registry setup, including base path and entities.
   * @param publisher the application event publisher used to publish events when entities are registered.
   * @return a configured instance of {@link EntityRecordRegistry} that delegates operations and publishes registration events.
   */
  @Bean
  EntityRecordRegistry entityRegistry(final EntityConfigurationProperties properties,
                                      final ApplicationEventPublisher publisher
  ) {

    final String subject = properties.getEntityRegistry()
        .stream()
        .filter(EntityProperty::isDefaultEntity)
        .findFirst()
        .get()
        .getSubject();
    return new DelegatingEntityRecordRegistry(
        new EntityID(subject)
        , new InMemoryEntityRecordRegistry(properties.getBasePath()),
        List.of(er -> publisher.publishEvent(new EntityRegisteredEvent(er))));
  }

  /**
   * Factory method to create an instance of {@link EntityStatementFactory}.
   *
   * @param registry the {@link KeyRegistry} to retrieve the signing key used for creating entity statements
   * @return an instance of {@link EntityStatementFactory} configured with the specified signing key
   */
  @Bean
  EntityStatementFactory entityStatementFactory(final KeyRegistry registry) {
    return new EntityStatementFactory(registry.getKey("sign-key-1").get());
  }

  /**
   * Creates and returns a {@link RestClient} instance for integrating with the entity record service.
   * The client is retrieved from the provided {@link RestClientRegistry} using the configuration details
   * from {@link EntityConfigurationProperties}.
   *
   * This method is only instantiated when the property 'openid.federation.entity-registry.client'
   * is enabled in the application configuration.
   *
   * @param registry the {@link RestClientRegistry} used to retrieve the appropriate {@link RestClient}.
   * @param properties configuration properties for the entity record client, encapsulated in
   *                   {@link EntityConfigurationProperties}.
   * @return a {@link RestClient} for the entity record service integration.
   * @throws IllegalStateException if the specified client cannot be retrieved from the registry.
   */
  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
  @Qualifier("entity-record-integration-client")
  RestClient entityRecordIntegrationClient(final RestClientRegistry registry,
                                           final EntityConfigurationProperties properties) {
    return registry.getClient(properties.getClient())
        .orElseThrow();
  }

  /**
   * Configures a RecordRegistryIntegration bean to integrate with an entity record system.
   * The integration combines a REST client and a verifier for trust mark subject records.
   *
   * @param client the REST client for communication with the entity record integration service
   * @param verifier the verifier used to validate trust mark subject records
   * @return the RecordRegistryIntegration instance that facilitates entity record operations
   */
  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
  RecordRegistryIntegration entityRecordIntegration(
      @Qualifier("entity-record-integration-client") final RestClient client,
      final TrustMarkSubjectRecordVerifier verifier) {
    return new RestClientRecordIntegration(client, verifier);
  }

  /**
   * Creates and configures a TrustMarkSubjectRecordVerifier bean.
   *
   * @param registry the KeyRegistry used to retrieve cryptographic keys for verification.
   * @param properties the EntityConfigurationProperties providing configuration details such as JWK aliases.
   * @return a TrustMarkSubjectRecordVerifier instance configured with the keys retrieved from the registry
   *         based on the provided properties.
   */
  @Bean
  TrustMarkSubjectRecordVerifier recordVerifier(
      final KeyRegistry registry, final EntityConfigurationProperties properties) {
    return new TrustMarkSubjectRecordVerifier(registry.getSet(properties.getJwkAlias()));
  }

  /**
   * Creates and returns a {@link RecordRegistrySource} bean, configured with caching functionality if the
   * "openid.federation.entity-registry.client" property is enabled.
   *
   * @param integration the {@link RecordRegistryIntegration} instance used to fetch records from the registry.
   * @param cache the {@link RecordRegistryCache} instance used to cache registry records.
   * @return a {@link RecordRegistrySource} instance utilizing the provided integration and cache.
   */
  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
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
      @Override
      public Optional<PolicyRecord> getPolicy(final String id) {
        return Optional.empty();
      }

      @Override
      public List<EntityRecord> getEntityRecords(final String issuer) {
        return List.of();
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
}
