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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.CachedEntityRecordRegistry;
import se.digg.oidfed.common.entity.DelegatingEntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityConfigurationFactory;
import se.digg.oidfed.common.entity.EntityPathFactory;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.InMemoryMultiKeyCache;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.service.cache.CacheFactory;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;

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
   * @param factory    for creating caches for this component
   * @return a configured instance of {@link EntityRecordRegistry} that delegates operations and publishes registration
   * events.
   */
  @Bean
  EntityRecordRegistry entityRegistry(final OpenIdFederationConfigurationProperties properties,
                                      final ApplicationEventPublisher publisher,
                                      final CacheFactory factory
  ) {

    return new DelegatingEntityRecordRegistry(
        new CachedEntityRecordRegistry(
            new EntityPathFactory(Optional.ofNullable(properties.getModules())
                .map(OpenIdFederationConfigurationProperties.Modules::getIssuers)
                .orElse(List.of())),
            factory.createMultiKeyCache(EntityRecord.class)
        ),
        List.of(er -> publisher.publishEvent(new EntityRegisteredEvent(er))));
  }

  /**
   * Factory method to create an instance of {@link EntityConfigurationFactory}.
   *
   * @param factory for signing
   * @param client  for fetching trust marks
   * @return an instance of {@link EntityConfigurationFactory} configured with the specified signing key
   */
  @Bean
  EntityConfigurationFactory entityStatementFactory(final SignerFactory factory,
                                                    final FederationClient client
  ) {
    return new EntityConfigurationFactory(factory, client);
  }
}
