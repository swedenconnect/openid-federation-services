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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.resolver.TestEntitiesFactory;

import java.util.Date;
import java.util.List;

import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.IM_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;

/**
 * Tests for {@link SignatureValidationStep}.
 *
 * @author Felix Hellman
 */
class SignatureValidationStepTest {

  @Test
  void linkErrorTellsWhichKeyIsMissing() throws Exception {
    final TestEntitiesFactory entities = new TestEntitiesFactory();
    final SignedJWT subordinate = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
        .issuer(IM_ID)
        .subject(LEAF_ID)
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 60_000))
        .claim("jwks", new JWKSet(entities.samlSpKey.toPublicJWK()).toJSONObject())
        .build());

    final List<ChainValidationError> errors =
        new SignatureValidationStep(new JWKSet(entities.taKey.toPublicJWK()))
            .validate(List.of(entities.leafEC, subordinate, entities.taEC));

    final String message = errors.stream()
        .filter(e -> e.getErrorType() == ChainValidationErrorType.CHAIN_LINK_SIGNATURE_INVALID)
        .map(Throwable::getMessage)
        .findFirst()
        .orElseThrow();
    Assertions.assertTrue(message.contains("key '%s' used to sign the Entity Configuration of %s is not present in "
        .formatted(entities.leafKey.getKeyID(), LEAF_ID)
        + "the jwks of the Subordinate Statement issued by %s about %s".formatted(IM_ID, LEAF_ID)), message);
  }
}
