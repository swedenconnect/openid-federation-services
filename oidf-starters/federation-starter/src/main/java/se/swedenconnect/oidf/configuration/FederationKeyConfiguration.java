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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.SerialKeyIdAlgorithm;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

/**
 * Configures key related settings.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(KeyConfigurationProperties.class)
public class FederationKeyConfiguration {

  @Bean
  JwkTransformerFunction kidAlgorithm(final KeyConfigurationProperties properties) {
    final JwkTransformerFunction function = new JwkTransformerFunction();
    if (KeyConfigurationProperties.KeyIdAlgorithmType.SERIAL.equals(properties.getKidAlgorithm())) {
      return SerialKeyIdAlgorithm.setKeyIdAlgorithm(function);
    }
    return function;
  }

  @Bean
  KeyRegistry keyRegistry(final CredentialBundles bundles,
                          final JwkTransformerFunction jwkTransformerFunction,
                          final KeyConfigurationProperties properties) {
    final KeyRegistry keyRegistry = new KeyRegistry();

    jwkTransformerFunction
        .withRsaCustomizer(rsa -> rsa.x509CertChain(null))
        .withEcKeyCustomizer(ec -> ec.x509CertChain(null))
        .serializable();

    bundles.getRegisteredCredentials().forEach(key -> {
      final KeyProperty property = new KeyProperty();
      final PkiCredential credential = bundles.getCredential(key);
      property.setKey(jwkTransformerFunction.apply(credential));
      property.setAlias(key);
      property.setMapping(properties.getMapping(property));
      keyRegistry.register(property);
    });
    properties.getAdditionalKeys()
        .forEach(key -> {
          final JWK parsed = key.getKey();
          final KeyProperty property = new KeyProperty();
          property.setKey(parsed);
          property.setAlias(key.name());
          property.setMapping("public");
          keyRegistry.register(property);
        });

    properties.getMapping().forEach((mapping, list) -> {
      list.forEach(key -> {
        keyRegistry.getKey("%s:%s".formatted(mapping, key)).orElseThrow(
            () -> {
              final String message = "An unknown private key was mapped %s to %s, remove this mapping or load the key";
              return new IllegalArgumentException(message.formatted(key, mapping));
            });
      });
    });

    return keyRegistry;
  }
}
