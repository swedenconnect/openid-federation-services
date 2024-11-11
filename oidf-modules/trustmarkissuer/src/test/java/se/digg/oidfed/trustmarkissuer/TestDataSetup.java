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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Data setup for testing
 *
 * @author Per Fredrik Plars
 */
public class TestDataSetup {



  public static TrustMarkProperties trustMarkProperties() throws JOSEException {
    final TrustMarkProperties trustMarkProperties = new TrustMarkProperties();
    trustMarkProperties.setTrustMarkValidityDuration(Duration.of(5, ChronoUnit.MINUTES));
    trustMarkProperties.setIssuerEntityId("https://tmissuer.digg.se");
    trustMarkProperties.setSignKey(generateKey() );
    trustMarkProperties.setTrustMarks(new ArrayList<>());

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub1 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://tm1.digg.se/sub1")
            .expires(Instant.now().plus(10, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    trustMarkProperties.getTrustMarks()
        .add(TrustMarkIssuerProperties.builder()
            .trustMarkId(TrustMarkId.create("http://tm.digg.se/default"))
            .subjects(List.of(sub1))
            .build());

    return trustMarkProperties;
  }


  private static String generateKey() throws JOSEException, JOSEException {
    final RSAKey rsaKey = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();
    return Base64.getEncoder().encodeToString(rsaKey.toJSONString().getBytes(Charset.defaultCharset()));
  }

  public static JWK jwkSignKey()
      throws Exception {

    final String password = "Test1234";
    final String alias = "rsa1";
    final String keyStoreFile = "/rsa1.jks";

    try (InputStream keyStoreStream = TestDataSetup.class.getResourceAsStream(keyStoreFile)) {
      if (keyStoreStream == null) {
        throw new IllegalArgumentException("KeyStore not found: " + keyStoreFile);
      }

      final KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(keyStoreStream, password.toCharArray());

      final JWK key = JWK.load(keyStore, alias, password.toCharArray());
      return key;
    }

  }

}
