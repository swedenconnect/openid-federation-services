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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for reading and writing trust mark entries in {@link EntityStatementClaims}.
 *
 * @author Martin Lindström
 */
class EntityStatementClaimsTrustMarksTest {

  private static final String SUBJECT = "https://example.com/entity";
  private static final String TYPE_1 = "https://example.com/tm/1";
  private static final String TYPE_2 = "https://example.com/tm/2";

  private ECKey key;

  private String trustMark;

  @BeforeEach
  void setUp() throws Exception {
    this.key = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
    this.trustMark = this.sign(new JWTClaimsSet.Builder()
        .issuer("https://example.com/tmi")
        .subject(SUBJECT)
        .claim("trust_mark_type", TYPE_1)
        .build()).serialize();
  }

  @Test
  void readsTrustMarkType() throws Exception {
    final List<TrustMarkEntry> marks = EntityStatementClaims.getTrustMarks(
        this.entityStatement(List.of(Map.of("trust_mark_type", TYPE_1, "trust_mark", this.trustMark))));

    Assertions.assertEquals(1, marks.size());
    Assertions.assertEquals(TYPE_1, marks.getFirst().getID().getValue());
    Assertions.assertEquals(this.trustMark, marks.getFirst().getTrustMark().serialize());
  }

  @Test
  void fallsBackToDraftId() throws Exception {
    final List<TrustMarkEntry> marks = EntityStatementClaims.getTrustMarks(
        this.entityStatement(List.of(Map.of("id", TYPE_1, "trust_mark", this.trustMark))));

    Assertions.assertEquals(1, marks.size());
    Assertions.assertEquals(TYPE_1, marks.getFirst().getID().getValue());
  }

  @Test
  void prefersTrustMarkTypeOverId() throws Exception {
    final List<TrustMarkEntry> marks = EntityStatementClaims.getTrustMarks(this.entityStatement(
        List.of(Map.of("trust_mark_type", TYPE_1, "id", TYPE_2, "trust_mark", this.trustMark))));

    Assertions.assertEquals(TYPE_1, marks.getFirst().getID().getValue());
  }

  @Test
  void skipsInvalidEntriesAndKeepsValidOnes() throws Exception {
    final List<TrustMarkEntry> marks = EntityStatementClaims.getTrustMarks(this.entityStatement(List.of(
        Map.of("trust_mark", this.trustMark),
        Map.of("trust_mark_type", TYPE_2, "trust_mark", "not-a-jwt"),
        Map.of("trust_mark_type", TYPE_1, "trust_mark", this.trustMark))));

    Assertions.assertEquals(1, marks.size());
    Assertions.assertEquals(TYPE_1, marks.getFirst().getID().getValue());
  }

  @Test
  void returnsNullWithoutClaim() throws Exception {
    Assertions.assertNull(EntityStatementClaims.getTrustMarks(this.entityStatement(null)));
  }

  @Test
  void writesTrustMarkTypeAndId() throws Exception {
    final JSONObject json = EntityStatementClaims.toJSONObject(
        new TrustMarkEntry(new Identifier(TYPE_1), SignedJWT.parse(this.trustMark)));

    Assertions.assertEquals(TYPE_1, json.get("trust_mark_type"));
    Assertions.assertEquals(TYPE_1, json.get("id"));
    Assertions.assertEquals(this.trustMark, json.get("trust_mark"));
  }

  @Test
  void trustMarkTypeFromTrustMarkJwt() throws Exception {
    Assertions.assertEquals(TYPE_1, EntityStatementClaims.getTrustMarkType(SignedJWT.parse(this.trustMark)));
    Assertions.assertEquals(TYPE_2, EntityStatementClaims.getTrustMarkType(
        this.sign(new JWTClaimsSet.Builder().claim("id", TYPE_2).build())));
    Assertions.assertNull(EntityStatementClaims.getTrustMarkType(
        this.sign(new JWTClaimsSet.Builder().issuer(SUBJECT).build())));
  }

  @Test
  void wrapperSkipsInvalidEntries() throws Exception {
    final List<SignedJWT> marks = new EntityStatementWrapper(this.entityStatement(List.of(
        Map.of("trust_mark_type", TYPE_2, "trust_mark", "not-a-jwt"),
        Map.of("trust_mark_type", TYPE_1, "trust_mark", this.trustMark)))).getTrustMarks();

    Assertions.assertEquals(1, marks.size());
    Assertions.assertEquals(this.trustMark, marks.getFirst().serialize());
  }

  private SignedJWT entityStatement(final List<Map<String, Object>> trustMarks) throws Exception {
    final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(SUBJECT).subject(SUBJECT);
    if (trustMarks != null) {
      builder.claim("trust_marks", trustMarks);
    }
    return this.sign(builder.build());
  }

  private SignedJWT sign(final JWTClaimsSet claims) throws Exception {
    final SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(this.key.getKeyID()).build(), claims);
    jwt.sign(new ECDSASigner(this.key));
    return jwt;
  }
}
