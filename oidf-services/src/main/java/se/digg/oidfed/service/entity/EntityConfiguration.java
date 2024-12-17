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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import se.digg.oidfed.common.entity.RecordVerifier;
import se.digg.oidfed.common.entity.EntityStatementFactory;
import se.digg.oidfed.common.entity.InMemoryEntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.InMemoryRecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.CachedRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.rest.RestClientRegistry;

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

  @Bean
  EntityStatementFactory entityStatementFactory(final KeyRegistry registry) {
    return new EntityStatementFactory(registry.getKey("sign-key-1").get());
  }

  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
  @Qualifier("entity-record-integration-client")
  RestClient entityRecordIntegrationClient(final RestClientRegistry registry,
                                           final EntityConfigurationProperties properties) {
    return registry.getClient(properties.getClient())
        .orElseThrow();
  }

  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
  RecordRegistryIntegration entityRecordIntegration(
      @Qualifier("entity-record-integration-client") final RestClient client, final RecordVerifier verifier) {
    return new RestClientRecordIntegration(client, verifier);
  }

  @Bean
  RecordVerifier entityRecordVerifier(
      final KeyRegistry registry, final EntityConfigurationProperties properties) {
    return new RecordVerifier(registry.getSet(properties.getJwkAlias()));
  }

  @Bean
  @ConditionalOnProperty(value = "openid.federation.entity-registry.client")
  RecordRegistrySource recordRegistrySource(
      final RecordRegistryIntegration integration,
      final RecordRegistryCache cache) {
    return new CachedRecordRegistrySource(integration, cache);
  }

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

  @Bean
  RecordRegistryCache recordRegistryCache() {
    return new InMemoryRecordRegistryCache();
  }
}
