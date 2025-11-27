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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.service.entity.registry.RestClientRecordIntegration;
import se.swedenconnect.oidf.service.rest.RestClientFactory;
import se.swedenconnect.oidf.service.rest.RestClientProperties;

/**
 * Configuration for running towards local configuration AND/OR a registry service.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    name = "openid.federation.registry.integration.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class RegistryConfiguration {

  @Bean
  @Qualifier("registry-client")
  RestClient registryClient(final RestClientFactory factory, final OpenIdFederationConfigurationProperties properties) {
    final RestClientProperties.RestClientProperty property = new RestClientProperties.RestClientProperty();
    property.setTrustStoreBundleName("oidf-internal");
    property.setName("registry-client");
    property.setBaseUri(properties.getRegistry().getIntegration().getEndpoints().getBasePath());
    return factory.create(property);
  }

  @Bean
  RecordRegistryIntegration recordRegistryIntegration(final RegistryVerifier verifier,
                                                      @Qualifier("registry-client") final RestClient client) {
    return new RestClientRecordIntegration(verifier, client);
  }
}
