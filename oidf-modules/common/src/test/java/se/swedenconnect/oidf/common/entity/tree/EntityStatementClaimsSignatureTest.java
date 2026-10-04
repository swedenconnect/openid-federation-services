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
package se.swedenconnect.oidf.common.entity.tree;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

/**
 * Tests for the {@code kid} checks in {@link EntityStatementClaims#verifySignature(SignedJWT, JWKSet)}.
 *
 * @author Martin Lindström
 */
class EntityStatementClaimsSignatureTest {

  private ECKey key;

  @BeforeEach
  void setUp() throws Exception {
    this.key = new ECKeyGenerator(Curve.P_256).keyID("key-1").generate();
  }

  @Test
  void statementWithMatchingKidIsAccepted() throws Exception {
    Assertions.assertNotNull(EntityStatementClaims.verifySignature(this.sign("key-1"), this.jwks()));
  }

  @Test
  void statementWithoutKidIsRejected() throws Exception {
    final SignedJWT jwt = this.sign(null);
    Assertions.assertThrows(BadJOSEException.class, () -> EntityStatementClaims.verifySignature(jwt, this.jwks()));
  }

  @Test
  void statementWithUnknownKidIsRejected() throws Exception {
    final SignedJWT jwt = this.sign("other");
    Assertions.assertThrows(BadJOSEException.class, () -> EntityStatementClaims.verifySignature(jwt, this.jwks()));
  }

  private JWKSet jwks() {
    return new JWKSet(this.key.toPublicJWK());
  }

  private SignedJWT sign(final String kid) throws Exception {
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(kid)
        .build();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer("https://example.com")
        .subject("https://example.com")
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new ECDSASigner(this.key));
    return jwt;
  }
}
