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
package se.swedenconnect.oidf.resolver.metadata;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.ValueOperation;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

class MetadataProcessorTest {

  private static final String LEAF_ID = "https://leaf.example.com";
  private static final String SUPERIOR_ID = "https://superior.example.com";

  private final MetadataProcessor processor =
      new MetadataProcessor(new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator());

  @Test
  void superiorMetadataAddsKeyLeafDidNotSet() throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));
    final SignedJWT superiorStatement = subordinateStatement(
        leafKey, SUPERIOR_ID, LEAF_ID, metadata(java.util.Map.of("logo_uri", "https://superior.example.com/logo.png")), null);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    final JSONObject rp = (JSONObject) result.get("openid_relying_party");
    Assertions.assertEquals("Leaf Org", rp.get("organization_name"));
    Assertions.assertEquals("https://superior.example.com/logo.png", rp.get("logo_uri"));
  }

  @Test
  void superiorMetadataOverridesLeafValueForSameKey() throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));
    final SignedJWT superiorStatement = subordinateStatement(
        leafKey, SUPERIOR_ID, LEAF_ID, metadata(java.util.Map.of("organization_name", "Superior Org")), null);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    final JSONObject rp = (JSONObject) result.get("openid_relying_party");
    Assertions.assertEquals("Superior Org", rp.get("organization_name"));
  }

  @Test
  void noSuperiorMetadataClaimBehavesAsBefore() throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));
    final SignedJWT superiorStatement = subordinateStatement(leafKey, SUPERIOR_ID, LEAF_ID, null, null);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    final JSONObject rp = (JSONObject) result.get("openid_relying_party");
    Assertions.assertEquals("Leaf Org", rp.get("organization_name"));
  }

  @Test
  void metadataPolicyValueOperatorWinsOverSuperiorMetadata() throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));

    final ValueOperation valueOperation = new ValueOperation();
    valueOperation.configure("Policy Org");
    final MetadataPolicy policy = new MetadataPolicy();
    policy.put("organization_name", List.of(valueOperation));
    final JSONObject metadataPolicy = new JSONObject();
    metadataPolicy.put("openid_relying_party", policy.toJSONObject());

    final SignedJWT superiorStatement = subordinateStatement(
        leafKey, SUPERIOR_ID, LEAF_ID, metadata(java.util.Map.of("organization_name", "Superior Org")), metadataPolicy);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    final JSONObject rp = (JSONObject) result.get("openid_relying_party");
    Assertions.assertEquals("Policy Org", rp.get("organization_name"));
  }

  @Test
  void selfIssuedOnlyChainDoesNotThrow() throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));

    final JSONObject result = this.processor.processMetadata(List.of(leafEc));

    final JSONObject rp = (JSONObject) result.get("openid_relying_party");
    Assertions.assertEquals("Leaf Org", rp.get("organization_name"));
  }

  @Test
  void regexpOperatorIsApplied() throws Exception {
    final JSONObject policy = policy("organization_name", java.util.Map.of("regexp", List.of("^Policy.*")));
    final IllegalArgumentException e =
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.processWithPolicy(policy, null));
    Assertions.assertInstanceOf(PolicyViolationException.class, e.getCause());
  }

  @Test
  void unknownOperatorIsIgnored() throws Exception {
    final JSONObject policy = policy("organization_name",
        java.util.Map.of("value", "Policy Org", "unknown_op", "anything"));
    final JSONObject rp = (JSONObject) this.processWithPolicy(policy, null).get("openid_relying_party");
    Assertions.assertEquals("Policy Org", rp.get("organization_name"));
  }

  @Test
  void unknownCriticalOperatorFails() throws Exception {
    final JSONObject policy = policy("organization_name",
        java.util.Map.of("value", "Policy Org", "unknown_op", "anything"));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.processWithPolicy(policy, List.of("unknown_op")));
  }

  @Test
  void invalidOperatorCombinationFails() throws Exception {
    final JSONObject policy = policy("organization_name",
        java.util.Map.of("value", "Policy Org", "default", "Default Org"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.processWithPolicy(policy, null));
  }

  @Test
  void malformedPolicyFails() throws Exception {
    final JSONObject policy = new JSONObject();
    policy.put("openid_relying_party", new JSONObject(java.util.Map.of("organization_name", "not an object")));
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.processWithPolicy(policy, null));
  }

  @Test
  void policiesAreAppliedPerEntityType() throws Exception {
    final JWK leafKey = generateKey();
    final JSONObject metadata = metadata(java.util.Map.of("organization_name", "Leaf Org"));
    metadata.put("federation_entity", new JSONObject(java.util.Map.of("organization_name", "Leaf Org")));
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata);

    final JSONObject policy = policy("organization_name", java.util.Map.of("value", "RP Org"));
    policy.put("federation_entity",
        new JSONObject(java.util.Map.of("organization_name", new JSONObject(java.util.Map.of("value", "Fed Org")))));
    final SignedJWT superiorStatement = subordinateStatement(leafKey, SUPERIOR_ID, LEAF_ID, null, policy);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    Assertions.assertEquals("RP Org", ((JSONObject) result.get("openid_relying_party")).get("organization_name"));
    Assertions.assertEquals("Fed Org", ((JSONObject) result.get("federation_entity")).get("organization_name"));
  }

  @Test
  void policyForOneTypeIsNotAppliedToAnother() throws Exception {
    final JWK leafKey = generateKey();
    final JSONObject metadata = metadata(java.util.Map.of("organization_name", "Leaf Org"));
    metadata.put("federation_entity", new JSONObject(java.util.Map.of("organization_name", "Fed Leaf Org")));
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata);

    final JSONObject policy = policy("organization_name", java.util.Map.of("value", "RP Org"));
    final SignedJWT superiorStatement = subordinateStatement(leafKey, SUPERIOR_ID, LEAF_ID, null, policy);

    final JSONObject result = this.processor.processMetadata(List.of(leafEc, superiorStatement));

    Assertions.assertEquals("RP Org", ((JSONObject) result.get("openid_relying_party")).get("organization_name"));
    Assertions.assertEquals("Fed Leaf Org",
        ((JSONObject) result.get("federation_entity")).get("organization_name"));
  }

  @Test
  void typesNotAllowedAreRemoved() throws Exception {
    final JSONObject result = this.processWithAllowedTypes(List.of(List.of("openid_provider")));
    Assertions.assertEquals(java.util.Set.of("federation_entity"), result.keySet());
  }

  @Test
  void emptyAllowedTypesKeepsOnlyFederationEntity() throws Exception {
    final JSONObject result = this.processWithAllowedTypes(List.of(List.of()));
    Assertions.assertEquals(java.util.Set.of("federation_entity"), result.keySet());
  }

  @Test
  void allowedTypesAreKept() throws Exception {
    final JSONObject result = this.processWithAllowedTypes(List.of(List.of("openid_relying_party")));
    Assertions.assertEquals(java.util.Set.of("federation_entity", "openid_relying_party"), result.keySet());
  }

  @Test
  void allowedTypesOfAllStatementsMustMatch() throws Exception {
    final JSONObject result = this.processWithAllowedTypes(
        List.of(List.of("openid_relying_party"), List.of("openid_provider")));
    Assertions.assertEquals(java.util.Set.of("federation_entity"), result.keySet());
  }

  private JSONObject processWithAllowedTypes(final List<List<String>> allowedTypesPerStatement) throws Exception {
    final JWK leafKey = generateKey();
    final JSONObject metadata = metadata(java.util.Map.of("organization_name", "Leaf Org"));
    metadata.put("federation_entity", new JSONObject(java.util.Map.of("organization_name", "Leaf Org")));
    final List<SignedJWT> chain = new java.util.ArrayList<>();
    chain.add(selfStatement(leafKey, LEAF_ID, metadata));
    for (final List<String> allowedTypes : allowedTypesPerStatement) {
      final JWK key = generateKey();
      final JWTClaimsSet claims = new JWTClaimsSet.Builder()
          .issuer(SUPERIOR_ID)
          .subject(LEAF_ID)
          .issueTime(Date.from(Instant.now()))
          .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
          .claim("jwks", new JSONObject(new JWKSet(leafKey.toPublicJWK()).toJSONObject()))
          .claim("constraints", new JSONObject(java.util.Map.of("allowed_entity_types", allowedTypes)))
          .build();
      final SignedJWT statement = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
          .type(new JOSEObjectType("entity-statement+jwt")).keyID(key.getKeyID()).build(), claims);
      statement.sign(new RSASSASigner(key.toRSAKey()));
      chain.add(statement);
    }
    return this.processor.processMetadata(chain);
  }

  private JSONObject processWithPolicy(final JSONObject metadataPolicy, final List<String> metadataPolicyCrit)
      throws Exception {
    final JWK leafKey = generateKey();
    final SignedJWT leafEc = selfStatement(leafKey, LEAF_ID, metadata(java.util.Map.of(
        "organization_name", "Leaf Org"
    )));
    final SignedJWT superiorStatement =
        subordinateStatement(leafKey, SUPERIOR_ID, LEAF_ID, null, metadataPolicy, metadataPolicyCrit);
    return this.processor.processMetadata(List.of(leafEc, superiorStatement));
  }

  private static JSONObject policy(final String parameter, final java.util.Map<String, Object> operators) {
    final JSONObject typePolicy = new JSONObject();
    typePolicy.put(parameter, new JSONObject(operators));
    final JSONObject policy = new JSONObject();
    policy.put("openid_relying_party", typePolicy);
    return policy;
  }

  private static JWK generateKey() throws Exception {
    return new RSAKeyGenerator(2048).keyID("key").generate();
  }

  private static JSONObject metadata(final java.util.Map<String, Object> rpMetadata) {
    final JSONObject metadata = new JSONObject();
    metadata.put("openid_relying_party", new JSONObject(rpMetadata));
    return metadata;
  }

  private static SignedJWT selfStatement(final JWK key, final String entityId, final JSONObject metadata)
      throws Exception {
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(key.getKeyID())
        .build();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
        .claim("jwks", new JSONObject(new JWKSet(key.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata)
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt;
  }

  private static SignedJWT subordinateStatement(
      final JWK subjectKey, final String issuer, final String subject,
      final JSONObject metadata, final JSONObject metadataPolicy) throws Exception {
    return subordinateStatement(subjectKey, issuer, subject, metadata, metadataPolicy, null);
  }

  private static SignedJWT subordinateStatement(
      final JWK subjectKey, final String issuer, final String subject,
      final JSONObject metadata, final JSONObject metadataPolicy, final List<String> metadataPolicyCrit)
      throws Exception {
    final JWK signingKey = generateKey();
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(signingKey.getKeyID())
        .build();
    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plus(Duration.ofDays(1))))
        .claim("jwks", new JSONObject(new JWKSet(subjectKey.toPublicJWK()).toJSONObject()));
    if (metadata != null) {
      claims.claim("metadata", metadata);
    }
    if (metadataPolicy != null) {
      claims.claim("metadata_policy", metadataPolicy);
    }
    if (metadataPolicyCrit != null) {
      claims.claim("metadata_policy_crit", metadataPolicyCrit);
    }
    final SignedJWT jwt = new SignedJWT(header, claims.build());
    jwt.sign(new RSASSASigner(signingKey.toRSAKey()));
    return jwt;
  }
}
