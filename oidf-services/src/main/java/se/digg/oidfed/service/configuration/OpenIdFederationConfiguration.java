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
package se.digg.oidfed.service.configuration;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.registry.FailableRecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.registry.JWSRegistryVerifier;
import se.digg.oidfed.common.entity.integration.registry.ModuleResponse;
import se.digg.oidfed.common.entity.integration.registry.RegistryRefreshAheadCache;
import se.digg.oidfed.common.entity.integration.registry.RefreshAheadRecordRegistrySource;
import se.digg.oidfed.common.entity.integration.registry.RegistryProperties;
import se.digg.oidfed.common.entity.integration.registry.RegistryVerifier;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.federation.FederationLoadingCache;
import se.digg.oidfed.common.entity.integration.registry.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubject;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.service.cache.CacheFactory;
import se.digg.oidfed.service.entity.PolicyConfigurationProperties;
import se.digg.oidfed.service.entity.RestClientFederationClient;
import se.digg.oidfed.service.entity.RestClientRecordIntegration;
import se.digg.oidfed.service.keys.FederationKeys;
import se.digg.oidfed.service.resolver.RestClientEntityStatementIntegration;
import se.digg.oidfed.service.rest.RestClientFactory;
import se.digg.oidfed.service.rest.RestClientProperties;
import se.digg.oidfed.service.trustanchor.TrustAnchorModuleProperties;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerModuleProperties;

/**
 * Configuration class for openid federation.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(OpenIdFederationConfigurationProperties.class)
public class OpenIdFederationConfiguration {

  @Bean
  EntityStatementIntegration integration(@Qualifier("module-client") final RestClient client) {
    return new RestClientEntityStatementIntegration(client);
  }

  @Bean
  RegistryVerifier registryVerifier(final FederationKeys keys) {
    return new JWSRegistryVerifier(keys.validationKeys());
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
  FederationClient federationClient(
      @Qualifier("module-client") final RestClient restClient,
      final CacheFactory factory) {
    return new FederationLoadingCache(new RestClientFederationClient(restClient),
        factory.create(EntityStatement.class),
        factory.create(EntityStatement.class),
        factory.createListValueCache(String.class),
        factory.create(SignedJWT.class),
        factory.create(SignedJWT.class),
        factory.createListValueCache(String.class)
    );
  }

  @Bean
  RefreshAheadRecordRegistrySource refreshAheadRecordRegistrySource(final RegistryProperties properties,
                                                                    final FailableRecordRegistryIntegration integration,
                                                                    final RegistryRefreshAheadCache cache) {
    return new RefreshAheadRecordRegistrySource(properties, integration, cache);
  }

  @Bean
  FailableRecordRegistryIntegration failableRecordRegistryIntegration(final RecordRegistryIntegration integration) {
    return new FailableRecordRegistryIntegration(integration);
  }

  @Bean
  RecordRegistryIntegration recordRegistryIntegration(final RegistryVerifier verifier,
                                                      @Qualifier("registry-client") final RestClient client) {
    return new RestClientRecordIntegration(verifier, client);
  }

  @Bean
  RegistryRefreshAheadCache refreshAheadCache(final CacheFactory factory) {
    return new RegistryRefreshAheadCache(
        factory.create(ModuleResponse.class),
        factory.createListValueCache(TrustMarkSubject.class),
        factory.createListValueCache(EntityRecord.class),
        factory.create(PolicyRecord.class)
    );
  }

  @Bean
  RegistryProperties registryProperties(
      final OpenIdFederationConfigurationProperties properties,
      final KeyRegistry registry,
      final FederationKeys keys
  ) {
    return new RegistryProperties(
        properties.getRegistry().getIntegration().getInstanceId(),
        properties.getModules().getTrustMarkIssuers()
            .stream()
            .map(TrustMarkIssuerModuleProperties.TrustMarkIssuerSubModuleProperty::toProperties)
            .toList(),
        properties.getModules().getTrustAnchors()
            .stream().map(TrustAnchorModuleProperties.TrustAnchorSubModuleProperties::toTrustAnchorProperties)
            .toList(),
        properties.getModules().getResolvers().stream().map(r -> r.toResolverProperties(registry)).toList(),
        properties.getEntities().stream().map(e -> e.toEntityRecord(registry, keys)).toList(),
        properties.getPolicies().stream().map(PolicyConfigurationProperties.PolicyRecordProperty::toRecord).toList()
    );
  }

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
  RestClientFactory restClientFactory(final SslBundles bundles) {
    return new RestClientFactory(bundles);
  }


}
