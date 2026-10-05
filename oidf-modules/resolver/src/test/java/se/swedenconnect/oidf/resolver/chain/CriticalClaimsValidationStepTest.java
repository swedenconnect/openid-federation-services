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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CriticalClaimsValidationStepTest {

  private final CriticalClaimsValidationStep step = new CriticalClaimsValidationStep();

  @Test
  void supportedCritIsAccepted() {
    Assertions.assertDoesNotThrow(() -> this.step.validate(List.of(statement("crit", List.of("ec_location")))));
  }

  @Test
  void unknownCritClaimIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.step.validate(List.of(statement("crit", List.of("ec_location", "foo")))));
  }

  @Test
  void specDefinedCritClaimIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.step.validate(List.of(statement("crit", List.of("metadata")))));
  }

  @Test
  void unknownOperatorIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.step.validate(
        List.of(statement("metadata_policy_crit", List.of("unknown_op")))));
  }

  @Test
  void standardOperatorIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.step.validate(List.of(statement("metadata_policy_crit", List.of("value")))));
  }

  @Test
  void emptyMetadataPolicyCritIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.step.validate(List.of(statement("metadata_policy_crit", List.of()))));
  }

  @Test
  void metadataPolicyCritInEntityConfigurationIsRejected() {
    final SignedJWT entityConfiguration = statement("https://example.com/leaf", "metadata_policy_crit",
        List.of("unknown_op"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.step.validate(List.of(entityConfiguration)));
  }

  private static SignedJWT statement(final String claim, final List<String> value) {
    return statement("https://example.com/ta", claim, value);
  }

  private static SignedJWT statement(final String issuer, final String claim, final List<String> value) {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject("https://example.com/leaf")
        .claim(claim, value)
        .build();
    return new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
  }
}
