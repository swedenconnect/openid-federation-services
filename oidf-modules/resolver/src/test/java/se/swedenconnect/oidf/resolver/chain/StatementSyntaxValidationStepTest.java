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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link StatementSyntaxValidationStep}.
 *
 * @author Martin Lindström
 */
class StatementSyntaxValidationStepTest {

  private static final String LEAF = "https://leaf.example.com";
  private static final String SUPERIOR = "https://superior.example.com";

  private final StatementSyntaxValidationStep step = new StatementSyntaxValidationStep();

  @Test
  void wellFormedStatementsAreAccepted() {
    Assertions.assertTrue(this.validate(this.configuration(Map.of(
        "authority_hints", List.of(SUPERIOR),
        "metadata", Map.of("openid_relying_party", Map.of("client_name", "RP")))),
        this.subordinate(Map.of(
            "constraints", Map.of("max_path_length", 1, "allowed_entity_types", List.of("openid_relying_party")),
            "metadata_policy", Map.of("openid_relying_party", Map.of("contacts", Map.of("add", List.of("a")))),
            "source_endpoint", "https://superior.example.com/fetch"))).isEmpty());
  }

  @Test
  void entityConfigurationClaimsInSubordinateStatementAreRejected() {
    for (final String claim : List.of("authority_hints", "trust_marks", "trust_mark_issuers", "trust_mark_owners",
        "trust_anchor_hints")) {
      Assertions.assertFalse(this.validate(this.subordinate(Map.of(claim, List.of()))).isEmpty(), claim);
    }
  }

  @Test
  void subordinateStatementClaimsInEntityConfigurationAreRejected() {
    for (final String claim : List.of("metadata_policy", "metadata_policy_crit", "constraints", "source_endpoint")) {
      Assertions.assertFalse(this.validate(this.configuration(Map.of(claim, Map.of()))).isEmpty(), claim);
    }
  }

  @Test
  void emptyAuthorityHintsAreRejected() {
    Assertions.assertFalse(this.validate(this.configuration(Map.of("authority_hints", List.of()))).isEmpty());
  }

  @Test
  void nullMetadataValueIsRejected() {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("client_name", null);
    Assertions.assertFalse(
        this.validate(this.configuration(Map.of("metadata", Map.of("openid_relying_party", parameters)))).isEmpty());
  }

  @Test
  void malformedConstraintsAreRejected() {
    Assertions.assertFalse(this.validate(this.subordinate(Map.of("constraints",
        Map.of("max_path_length", -1)))).isEmpty());
    Assertions.assertFalse(this.validate(this.subordinate(Map.of("constraints",
        Map.of("naming_constraints", Map.of("permitted", "example.com"))))).isEmpty());
  }

  @Test
  void malformedSourceEndpointIsRejected() {
    Assertions.assertFalse(this.validate(this.subordinate(Map.of("source_endpoint", "not a url"))).isEmpty());
  }

  private List<ChainValidationError> validate(final SignedJWT... statements) {
    return this.step.validate(List.of(statements));
  }

  private SignedJWT configuration(final Map<String, Object> claims) {
    return statement(LEAF, LEAF, claims);
  }

  private SignedJWT subordinate(final Map<String, Object> claims) {
    return statement(SUPERIOR, LEAF, claims);
  }

  private static SignedJWT statement(final String issuer, final String subject, final Map<String, Object> claims) {
    final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(issuer).subject(subject);
    claims.forEach(builder::claim);
    return new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build());
  }
}
