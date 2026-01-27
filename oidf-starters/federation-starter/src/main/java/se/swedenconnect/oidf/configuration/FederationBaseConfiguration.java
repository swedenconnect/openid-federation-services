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
package se.swedenconnect.oidf.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.FederationProperties;
import se.swedenconnect.oidf.RestClientFederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.LocalRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.RecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.jwt.JWKSetSignerFactory;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.routing.ErrorHandler;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;

import java.util.List;

/**
 * Base Configuration for OpenId Federation.
 *
 * @author Felix Hellman
 */
@Order(Integer.MAX_VALUE - 1)
@Configuration
@EnableConfigurationProperties(FederationProperties.class)
public class FederationBaseConfiguration {
  @Bean
  RecordSource propertyRecordSource(
      final FederationProperties properties
  ) {
    return new LocalRecordSource(properties.getLocalRegistry().toProperty());
  }

  @Bean
  CompositeRecordSource compositeRecordSource(final List<RecordSource> recordSources) {
    return new CompositeRecordSource(recordSources);
  }

  @Bean
  FederationClient federationClient(final RestClient restClient, final MeterRegistry registry) {
    return new RestClientFederationClient(restClient, registry);
  }

  @Bean
  SignerFactory signerFactory() {
    return new JWKSetSignerFactory();
  }

  @Bean
  RouteFactory routeFactory(final ServletContext context,
                            final FederationProperties properties) {
    return new RouteFactory(context, properties);
  }

  @Bean
  ServerResponseErrorHandler serverResponseErrorHandler() {
    return new ServerResponseErrorHandler(new ErrorHandler());
  }

  @Bean
  @ConfigurationPropertiesBinding
  JWKPropertyLoader jwkPropertyLoader(final ObjectProvider<KeyRegistry> registry) {
    return new JWKPropertyLoader(registry);
  }

  @Bean
  @ConfigurationPropertiesBinding
  JWKSPropertyLoader jwksPropertyLoader(final JWKPropertyLoader propertyLoader) {
    return new JWKSPropertyLoader(propertyLoader);
  }

  @Bean
  JsonReferenceLoader jsonReferenceLoader(
      final JWKPropertyLoader jwkPropertyLoader,
      final JWKSPropertyLoader jwksPropertyLoader) {
    return new JsonReferenceLoader(jwkPropertyLoader, jwksPropertyLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  PolicyRecordConverter policyRecordConverter(final JsonReferenceLoader jsonReferenceLoader) {
    return new PolicyRecordConverter(jsonReferenceLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  MetadataPropertyLoader metadataPropertyLoader(final JsonReferenceLoader jsonReferenceLoader) {
    return new MetadataPropertyLoader(jsonReferenceLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  EntityRecordPropertyLoader entityRecordPropertyLoader(final JsonReferenceLoader jsonReferenceLoader) {
    return new EntityRecordPropertyLoader(jsonReferenceLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  ResolverPropertyLoader resolverPropertyLoader(final JsonReferenceLoader jsonReferenceLoader) {
    return new ResolverPropertyLoader(jsonReferenceLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  TrustAnchorPropertyLoader trustAnchorPropertyLoader(final JsonReferenceLoader jsonReferenceLoader) {
    return new TrustAnchorPropertyLoader(jsonReferenceLoader);
  }

  @Bean
  @ConfigurationPropertiesBinding
  TrustMarkIssuerPropertyLoader trustMarkIssuerPropertyLoader(final JsonReferenceLoader jsonReferenceLoader) {
    return new TrustMarkIssuerPropertyLoader(jsonReferenceLoader);
  }
}
