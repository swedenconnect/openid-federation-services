/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.common.entity.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the algorithm choice of {@link JWKFederationSigner}.
 *
 * @author Martin Lindström
 */
class JWKFederationSignerTest {

  @Test
  void algOfKeyIsUsed() throws Exception {
    final JWKFederationSigner signer = new JWKFederationSigner(
        new RSAKeyGenerator(2048).keyID("rsa").algorithm(JWSAlgorithm.PS256).generate());
    final SignedJWT jwt = signer.sign(new JOSEObjectType("entity-statement+jwt"),
        new JWTClaimsSet.Builder().issuer("https://example.com").build());
    Assertions.assertEquals(JWSAlgorithm.PS256, jwt.getHeader().getAlgorithm());
    Assertions.assertTrue(signer.verify(jwt.serialize()));
  }

  @Test
  void defaultAlgorithmIsUsedWhenKeyHasNoAlg() throws Exception {
    final SignedJWT rsa = new JWKFederationSigner(new RSAKeyGenerator(2048).keyID("rsa").generate())
        .sign(new JOSEObjectType("entity-statement+jwt"), new JWTClaimsSet.Builder().build());
    final SignedJWT ec = new JWKFederationSigner(new ECKeyGenerator(Curve.P_256).keyID("ec").generate())
        .sign(new JOSEObjectType("entity-statement+jwt"), new JWTClaimsSet.Builder().build());
    Assertions.assertEquals(JWSAlgorithm.RS256, rsa.getHeader().getAlgorithm());
    Assertions.assertEquals(JWSAlgorithm.ES256, ec.getHeader().getAlgorithm());
  }
}
