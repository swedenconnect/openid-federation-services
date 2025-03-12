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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.EntityConfigurationFactory;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.PropertyRecordSource;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.jwt.SignerFactory;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
public class EntityConfiguration {
  /**
   * Factory method to create an instance of {@link EntityConfigurationFactory}.
   *
   * @param factory for signing
   * @param client  for fetching trust marks
   * @return an instance of {@link EntityConfigurationFactory} configured with the specified signing key
   */
  @Bean
  EntityConfigurationFactory entityStatementFactory(final SignerFactory factory,
                                                    final FederationClient client) {
    return new EntityConfigurationFactory(factory, client);
  }
}
