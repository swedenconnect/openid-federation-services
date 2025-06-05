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
import com.nimbusds.jose.jwk.RSAKey;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import java.security.cert.X509Certificate;

/**
 * KeyId Algorithm that derives kid from certificate serial number.
 *
 * @author Felix  Hellman
 */
public class SerialKeyIdAlgorithm {
  /**
   * @param function to modify
   * @return function
   */
  public static JwkTransformerFunction setKeyIdAlgorithm(final JwkTransformerFunction function) {
    return function
        .withRsaCustomizer(rsa -> {
          final RSAKey key = rsa.build();
          if (!key.getParsedX509CertChain().isEmpty()) {
            final X509Certificate certificate = key.getParsedX509CertChain().getFirst();
            rsa.keyID(certificate.getSerialNumber().toString(10));
          }
          return rsa;
        })
        .withEcKeyCustomizer(ec -> {
          final ECKey key = ec.build();
          if (!key.getParsedX509CertChain().isEmpty()) {
            final X509Certificate certificate = key.getParsedX509CertChain().getFirst();
            ec.keyID(certificate.getSerialNumber().toString(10));
          }
          return ec;
        });
  }
}
