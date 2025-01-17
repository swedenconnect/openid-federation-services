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
package se.digg.oidfed.service.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecordVerifier;
import se.digg.oidfed.common.entity.integration.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.TrustMarkIntegration;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.service.entity.RestClientRecordIntegration;
import se.digg.oidfed.service.entity.RestClientTrustMarkIntegration;
import se.digg.oidfed.service.modules.RestClientSubModuleIntegration;
import se.digg.oidfed.service.modules.SubModuleVerifier;
import se.digg.oidfed.service.resolver.RestClientEntityStatementIntegration;
import se.digg.oidfed.service.rest.RestClientFactory;
import se.digg.oidfed.service.rest.RestClientProperties;

/**
 * Configuration class for openid federation.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(OpenIdFederationConfigurationProperties.class)
public class OpenIdFederationConfiguration {

  @Bean
  TrustMarkIntegration trustMarkIntegration(@Qualifier("module-client") final RestClient client) {
    return new RestClientTrustMarkIntegration(client);
  }

  @Bean
  EntityStatementIntegration integration(@Qualifier("module-client") final RestClient client) {
    return new RestClientEntityStatementIntegration(client);
  }

  @Bean
  RecordRegistryIntegration entityRecordIntegration(
      @Qualifier("registry-client") final RestClient client,
      final EntityRecordVerifier verifier) {
    return new RestClientRecordIntegration(client, verifier);
  }

  @Bean
  RestClientSubModuleIntegration restClientSubModuleIntegration(
      @Qualifier("registry-client") final RestClient restClient,
      final SubModuleVerifier verifier) {
    return new RestClientSubModuleIntegration(restClient, verifier);
  }

  @Bean
  @Qualifier("module-client")
  RestClient resolverClient(final RestClientFactory factory, final OpenIdFederationConfigurationProperties properties) {
    final RestClientProperties.RestClientProperty property = new RestClientProperties.RestClientProperty();
    property.setTrustStoreBundleName("oidf-internal");
    property.setName("module-client");
    return factory.create(property);
  }

  @Bean
  @Qualifier("registry-client")
  RestClient regisryClient(final RestClientFactory factory, final OpenIdFederationConfigurationProperties properties) {
    final RestClientProperties.RestClientProperty property = new RestClientProperties.RestClientProperty();
    property.setTrustStoreBundleName("oidf-internal");
    property.setName("registry-client");
    property.setBaseUri(properties.getRegistry().getIntegration().getEndpoints().getBasePath());
    return factory.create(property);
  }

  @Bean
  RestClientFactory restClientFactory(final SslBundles bundles) {
    return new RestClientFactory(bundles);
  }


}
