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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.common.entity.RecordVerificationException;
import se.digg.oidfed.common.entity.integration.registry.JWSRegistryVerifier;
import se.digg.oidfed.common.entity.integration.registry.RegistryVerifier;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * openid-federation-services
 *
 * @author Per Fredrik Plars
 */
class RecordVerifierTest {

  @Test
  void verifyTrustMarkSubjects() throws JOSEException {

    final JWK key = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();

    final JWK key2 = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .issueTime(new Date())
        .generate();


    final JWK keySameKidAsKey = new RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(key.getKeyID())
        .issueTime(new Date())
        .generate();

    final JWKSet jwkSet = new JWKSet(List.of(key));

    final JWTClaimsSet claim = new JWTClaimsSet.Builder()
        .issueTime(new Date())
        .jwtID(UUID.randomUUID().toString())
        .issuer("http://myunittest.test.se")
        .claim("trustmark_records", List.of(Map.of("subject","http://sub.pm.se","revoked",true)))
        .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .build();

    final SignedJWT jwt = signJWT("trustmark_records",key,claim);

    final RegistryVerifier verifier = new JWSRegistryVerifier(jwkSet);
    final List<TrustMarkSubjectRecord> checkedResult = verifier.verifyTrustMarkSubjects(jwt.serialize()).getValue();
    assertEquals(1,checkedResult.size());
    assertEquals("http://sub.pm.se",checkedResult.get(0).trustMarkSubject());
    assertTrue(checkedResult.get(0).revoked());

    final SignedJWT jwt2 = signJWT("trustmark_records",key2,claim);
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,() ->
        verifier.verifyTrustMarkSubjects(jwt2.serialize()));
    assertTrue(ex.getMessage().contains("Unable to resolve key for JWT with kid:"));


    final SignedJWT jwt3 = signJWT("trustmark_records",keySameKidAsKey,claim);
    final RecordVerificationException signEx = assertThrows(RecordVerificationException.class,() ->
        verifier.verifyTrustMarkSubjects(jwt3.serialize()));

    assertTrue(signEx.getMessage().contains("Failed to verify signature on record"));

  }

  private SignedJWT signJWT(final String jwtTypeName,final JWK signKey,final JWTClaimsSet jwtClaimsSet)
      throws JOSEException {
    final RSASSASigner signer = new RSASSASigner(signKey.toRSAKey());
    final JWSAlgorithm alg = signer.supportedJWSAlgorithms().stream().findFirst().orElseThrow();
    final JWSHeader header = new JWSHeader.Builder(alg)
        .type(new JOSEObjectType(jwtTypeName.replace('_','-')+ "+jwt"))
        .keyID(signKey.getKeyID())
        .build();
    final SignedJWT jwt = new SignedJWT(header, jwtClaimsSet);
    jwt.sign(signer);
    return jwt;
  }

}