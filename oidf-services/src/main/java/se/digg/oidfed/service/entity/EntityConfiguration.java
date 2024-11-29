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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.DelegatingEntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityRecordIntegration;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.RecordVerifier;
import se.digg.oidfed.common.entity.EntityStatementFactory;
import se.digg.oidfed.common.entity.InMemoryEntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.InMemoryRecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryCache;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.rest.RestClientRegistry;

import java.util.List;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
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
  @Qualifier("entity-record-integration-client")
  RestClient entityRecordIntegrationClient(final RestClientRegistry registry,
                                           final EntityConfigurationProperties properties) {
    return registry.getClient(properties.getClient())
        .orElseThrow();
  }

  @Bean
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
  RecordRegistrySource recordRegistrySource(
      final RecordRegistryIntegration integration,
      final RecordRegistryCache cache) {
    return new RecordRegistrySource(integration, cache);
  }

  @Bean
  RecordRegistryCache recordRegistryCache() {
    return new InMemoryRecordRegistryCache();
  }
}
