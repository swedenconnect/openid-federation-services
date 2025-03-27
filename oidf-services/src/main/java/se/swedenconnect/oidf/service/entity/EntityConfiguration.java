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
package se.swedenconnect.oidf.service.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationClaimCustomizer;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.SigningEntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.util.List;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
public class EntityConfiguration {
  /**
   * Factory method to create an instance of {@link SigningEntityConfigurationFactory}.
   *
   * @param factory for signing
   * @param client  for fetching trust marks
   * @param customizers for customizing claims
   * @return an instance of {@link SigningEntityConfigurationFactory} configured with the specified signing key
   */
  @Bean
  EntityConfigurationFactory entityConfigurationFactory(final SignerFactory factory,
                                                        final FederationClient client,
                                                        final List<EntityConfigurationClaimCustomizer> customizers) {
    return new SigningEntityConfigurationFactory(factory, client, customizers);
  }

  @Bean
  TrustAnchorEntityCustomizer trustAnchorEntityCustomizer(final CompositeRecordSource source) {
    return new TrustAnchorEntityCustomizer(source);
  }
}
