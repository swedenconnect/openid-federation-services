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

import com.nimbusds.jose.jwk.JWK;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.FederationKeys;
import se.swedenconnect.oidf.OpenIdFederationProperties;
import se.swedenconnect.oidf.RestClientFederationClient;
import se.swedenconnect.oidf.SerialKeyIdAlgorithm;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.LocalRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.RecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.jwt.JWKSetSignerFactory;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(OpenIdFederationProperties.class)
public class FederationBaseConfiguration {
  @Bean
  KeyRegistry keyRegistry(final CredentialBundles bundles,
                          final OpenIdFederationProperties properties) {
    final KeyRegistry keyRegistry = new KeyRegistry();

    final JwkTransformerFunction jwkTransformerFunction = getTransformer(properties);

    jwkTransformerFunction
        .withRsaCustomizer(rsa -> rsa.x509CertChain(null))
        .withEcKeyCustomizer(ec -> ec.x509CertChain(null))
        .serializable();

    bundles.getRegisteredCredentials().forEach(key -> {
      final KeyProperty property = new KeyProperty();
      final PkiCredential credential = bundles.getCredential(key);
      property.setKey(jwkTransformerFunction.apply(credential));

      property.setAlias(key);
      keyRegistry.register(property);
    });
    properties.getAdditionalKeys()
        .forEach(key -> {
          final JWK parsed = key.getKey();
          final KeyProperty property = new KeyProperty();
          property.setKey(parsed);
          property.setAlias(key.name());
          keyRegistry.register(property);
        });

    return keyRegistry;
  }

  private static JwkTransformerFunction getTransformer(final OpenIdFederationProperties properties) {
    if (OpenIdFederationProperties.KeyIdAlgorithmType.SERIAL.equals(properties.getKidAlgorithm())) {
      return SerialKeyIdAlgorithm.setKeyIdAlgorithm(new JwkTransformerFunction());
    }

    return new JwkTransformerFunction();
  }

  @Bean
  FederationKeys federationKeys(
      final OpenIdFederationProperties properties,
      final KeyRegistry registry) {

    final List<String> validationKeys = Optional.ofNullable(properties.getRegistry())
        .flatMap(r -> Optional.ofNullable(r.getIntegration()))
        .flatMap(i -> Optional.ofNullable(i.getValidationKeys()))
        .orElse(List.of());

    return new FederationKeys(registry.getSet(properties.getSign()),
        registry.getSet(validationKeys));
  }

  @Bean
  RecordSource propertyRecordSource(final OpenIdFederationProperties properties) {
    return new LocalRecordSource(properties.getLocalRegistry());
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
  SignerFactory signerFactory(final FederationKeys keys) {
    return new JWKSetSignerFactory(keys.signKeys());
  }
}
