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
    final SignedJWT jwt = new SignedJWT(header, claims.build());
    jwt.sign(new RSASSASigner(signingKey.toRSAKey()));
    return jwt;
  }
}
