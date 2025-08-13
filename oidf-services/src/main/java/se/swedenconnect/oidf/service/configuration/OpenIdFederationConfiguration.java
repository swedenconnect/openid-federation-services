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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.common.entity.entity.integration.CacheRecordPopulator;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.LocalRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationLoadingCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.JWSRegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryRefreshAheadCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.service.cache.CacheFactory;
import se.swedenconnect.oidf.service.entity.PolicyConfigurationProperties;
import se.swedenconnect.oidf.service.entity.RestClientFederationClient;
import se.swedenconnect.oidf.service.keys.FederationKeys;
import se.swedenconnect.oidf.service.rest.RestClientFactory;
import se.swedenconnect.oidf.service.rest.RestClientProperties;
import se.swedenconnect.oidf.service.trustanchor.TrustAnchorModuleProperties;
import se.swedenconnect.oidf.service.trustmarkissuer.TrustMarkIssuerModuleConfigurationProperties;
import se.swedenconnect.oidf.service.trustmarkissuer.TrustMarkSubjectProperties;

import java.util.List;
import java.util.Optional;

/**
 * Configuration class for openid federation.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(OpenIdFederationConfigurationProperties.class)
public class OpenIdFederationConfiguration {

  @Bean
  FederationClient federationClient(
      @Qualifier("module-client") final RestClient client,
      final CacheFactory factory) {
    return new FederationLoadingCache(new RestClientFederationClient(client),
        factory.create(EntityStatement.class),
        factory.create(EntityStatement.class),
        factory.createListValueCache(String.class),
        factory.create(SignedJWT.class),
        factory.create(SignedJWT.class),
        factory.createListValueCache(String.class)
    );
  }

  @Bean
  RegistryVerifier registryVerifier(final FederationKeys keys) {
    return new JWSRegistryVerifier(keys.validationKeys());
  }

  @Bean
  @Qualifier("module-client")
  RestClient resolverClient(final RestClientFactory factory, final OpenIdFederationConfigurationProperties properties) {
    final RestClientProperties.RestClientProperty property = new RestClientProperties.RestClientProperty();
    Optional.ofNullable(properties.getTrustStoreName())
        .ifPresent(property::setTrustStoreBundleName);
    property.setName("module-client");
    return factory.create(property);
  }

  @Bean
  RegistryRefreshAheadCache refreshAheadCache(final CacheFactory factory) {
    return new RegistryRefreshAheadCache(
        factory.create(ModuleRecord.class),
        factory.createListValueCache(TrustMarkSubjectRecord.class),
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
    final OpenIdFederationConfigurationProperties.Modules modules =
        Optional.ofNullable(properties.getModules()).orElse(new OpenIdFederationConfigurationProperties.Modules());
    return new RegistryProperties(
        properties.getRedisKeyName(),
        properties.getRegistry().getIntegration().getEnabled(),
        Optional.ofNullable(modules.getTrustMarkIssuers())
            .orElse(List.of()).stream()
            .map(TrustMarkIssuerModuleConfigurationProperties.TrustMarkIssuerSubModuleProperty::toProperties)
            .toList(),
        Optional.ofNullable(modules.getTrustAnchors())
            .orElse(List.of()).stream()
            .map(TrustAnchorModuleProperties.TrustAnchorSubModuleProperties::toTrustAnchorProperties)
            .toList(),
        Optional.ofNullable(modules.getResolvers())
            .orElse(List.of()).stream()
            .map(r -> r.toResolverProperties(registry)).toList(),
        Optional.ofNullable(properties.getEntities())
            .orElse(List.of()).stream()
            .map(e -> e.toEntityRecord(registry, keys)).toList(),
        Optional.ofNullable(properties.getPolicies())
            .orElse(List.of())
            .stream().map(PolicyConfigurationProperties.PolicyRecordProperty::toRecord).toList()
        ,
        Optional.ofNullable(properties.getTrustMarkSubjects())
            .orElse(List.of()).stream()
            .map(TrustMarkSubjectProperties::toSubject).toList()
    );
  }

  @Bean
  RestClientFactory restClientFactory(final SslBundles bundles, final ObservationRegistry registry) {
    return new RestClientFactory(bundles, registry);
  }

  @Bean
  CompositeRecordSource compositeRecordSource(
      final LocalRecordSource localRecordSource,
      final CachedRecordSource cachedRecordSource) {
    return new CompositeRecordSource(
        List.of(
            localRecordSource,
            cachedRecordSource
        )
    );
  }

  @Bean
  CachedRecordSource cachedRecordSource(final CacheFactory factory) {
    return new CachedRecordSource(factory.create(CompositeRecord.class));
  }

  @Bean
  CacheRecordPopulator cacheRecordPopulator(
      final CachedRecordSource cachedRecordSource,
      final RecordRegistryIntegration recordRegistryIntegration,
      final OpenIdFederationConfigurationProperties properties
  ) {
    return new CacheRecordPopulator(
        cachedRecordSource,
        recordRegistryIntegration,
        properties.getRedisKeyName()
    );
  }

  @Bean
  LocalRecordSource propertyRecordSource(final RegistryProperties properties) {
    return new LocalRecordSource(properties);
  }
}
