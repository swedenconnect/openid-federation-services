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

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.Charset;
import java.security.PublicKey;

/**
 * @param name Name of key
 * @param base64EncodedPublicJwk Base 64 encoded jwk
 * @param certificates Pem certificate
 * @author Felix Hellman
 */
public record KeyEntry(String name, String base64EncodedPublicJwk, String certificates) {
  /**
   * @return parsed key
   */
  public JWK getKey() {
    try {
      if (this.certificates != null) {

        final JWK jwk = JWK.parseFromPEMEncodedObjects(this.certificates);
        return switch (jwk.getKeyType().getValue()) {
          case "RSA" ->
            // Om det är RSA, bygg om som RSAKey och sätt kid
              new RSAKey.Builder(((RSAKey) jwk).toRSAPublicKey())
                  .keyUse(jwk.getKeyUse())
                  .algorithm(jwk.getAlgorithm())
                  .keyIDFromThumbprint()
                  .build();
          case "EC" -> new ECKey.Builder(jwk.toECKey())
              .keyUse(jwk.getKeyUse())
              .algorithm(jwk.getAlgorithm())
              .keyIDFromThumbprint()
              .build();
          default -> throw new IllegalArgumentException("Unsupported key type: " + jwk.getKeyType());
        };
      }
      return JWK.parse(new String(Base64.decode(this.base64EncodedPublicJwk), Charset.defaultCharset()));
    }
    catch (final Exception e) {
      throw new RuntimeException("Failed to load additional key", e);
    }
  }

}
