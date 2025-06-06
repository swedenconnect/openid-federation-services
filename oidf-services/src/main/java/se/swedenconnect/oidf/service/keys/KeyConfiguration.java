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
package se.swedenconnect.oidf.service.keys;


import com.nimbusds.jose.jwk.JWK;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.service.configuration.OpenIdFederationConfigurationProperties;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

/**
 * Configuration class for keys and key-registry.
 *
 * @author Felix Hellman
 */
@Configuration
public class KeyConfiguration {
  @Bean
  KeyRegistry keyRegistry(final CredentialBundles bundles,
                          final OpenIdFederationConfigurationProperties properties) {
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

  private static JwkTransformerFunction getTransformer(final OpenIdFederationConfigurationProperties properties) {
    if (properties.getKidAlgorithm().equals("serial")) {
      return SerialKeyIdAlgorithm.setKeyIdAlgorithm(new JwkTransformerFunction());
    }

    return new JwkTransformerFunction();
  }

  @Bean
  FederationKeys federationKeys(
      final OpenIdFederationConfigurationProperties properties,
      final KeyRegistry registry) {
    return new FederationKeys(registry.getSet(properties.getSign()),
        registry.getSet(properties.getRegistry().getIntegration().getValidationKeys()));
  }

}
