/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import se.digg.oidfed.trustmarkissuer.configuration.ConfigurationResolverInMemory;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkProperties;

import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Data setup for testing
 *
 * @author Per Fredrik Plars
 */
public class TestDataSetup {

  public static ConfigurationResolverInMemory createConfigurationResolver() throws Exception {
    return new ConfigurationResolverInMemory(jwkSet(),
        TrustMarkProperties.builder()
            .trustMarkValidityDuration(Duration.of(5, ChronoUnit.MINUTES))
            .logoUri("http://www.digg.se/logo.png")
            .refUrl("http://www.digg.se/documentation/")
            .build(),
        "http://issuer.digg.se");
  }

  public static JWKSet jwkSet()
      throws Exception {

    final String password = "Test1234";
    final String alias = "rsa1";
    final String keyStoreFile = "/rsa1.jks";

    try (InputStream keyStoreStream = TestDataSetup.class.getResourceAsStream(keyStoreFile)) {
      if (keyStoreStream == null) {
        throw new IllegalArgumentException("KeyStore not found: " + keyStoreFile);
      }

      // Skapa en KeyStore av typen PKCS12
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(keyStoreStream, password.toCharArray());

      final JWK key = JWK.load(keyStore, alias, password.toCharArray());
      final JWKSet keys = new JWKSet(key);
      return keys;
    }

  }

}
