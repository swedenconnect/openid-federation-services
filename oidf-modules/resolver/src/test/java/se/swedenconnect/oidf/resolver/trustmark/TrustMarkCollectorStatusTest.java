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
package se.swedenconnect.oidf.resolver.trustmark;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

class TrustMarkCollectorStatusTest {

  private static final String TRUST_MARK_TYPE = "https://example.com/trustmark/type1";
  private static final String SUBJECT = "https://example.com/subject";
  private static final String ISSUER = "https://example.com/issuer";
  private static final String OWNER = "https://example.com/owner";
  private static final String TRUST_MARK_JWT_TYPE = "trust-mark+jwt";
  private static final String STATUS_TYPE = "trust-mark-status-response+jwt";

  private JWK leafKey;
  private JWK issuerKey;
  private Function<String, Optional<JWKSet>> issuerKeys;

  @BeforeEach
  void setUp() throws Exception {
    this.leafKey = new RSAKeyGenerator(2048).keyID("leaf-key").generate();
    this.issuerKey = new RSAKeyGenerator(2048).keyID("issuer-key").generate();
    this.issuerKeys = issuer -> ISSUER.equals(issuer)
        ? Optional.of(new JWKSet(this.issuerKey.toPublicJWK()))
        : Optional.empty();
  }

  @Test
  void activeTrustMarkIsCollected() throws Exception {
    final String trustMark = this.trustMark().build();
    Assertions.assertEquals(1, this.collect(trustMark, this.activeStatus(trustMark)).size());
  }

  @Test
  void inactiveTrustMarkIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    final SignedJWT status = this.status(this.issuerKey, STATUS_TYPE, ISSUER, trustMark, "revoked");
    Assertions.assertTrue(this.collect(trustMark, new TrustMarkStatusResponse(status, false)).isEmpty());
  }

  @Test
  void missingStatusIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    Assertions.assertTrue(this.collect(trustMark, null).isEmpty());
  }

  @Test
  void failedStatusCallIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    Assertions.assertTrue(this.collect(trustMark, new TrustMarkStatusResponse(null, true)).isEmpty());
  }

  @Test
  void statusNotSignedByIssuerIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    final JWK otherKey = new RSAKeyGenerator(2048).keyID("issuer-key").generate();
    final SignedJWT status = this.status(otherKey, STATUS_TYPE, ISSUER, trustMark, "active");
    Assertions.assertTrue(this.collect(trustMark, new TrustMarkStatusResponse(status, false)).isEmpty());
  }

  @Test
  void statusWithWrongTypIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    final SignedJWT status = this.status(this.issuerKey, "jwt", ISSUER, trustMark, "active");
    Assertions.assertTrue(this.collect(trustMark, new TrustMarkStatusResponse(status, false)).isEmpty());
  }

  @Test
  void statusFromOtherIssuerIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    final SignedJWT status =
        this.status(this.issuerKey, STATUS_TYPE, "https://example.com/other", trustMark, "active");
    Assertions.assertTrue(this.collect(trustMark, new TrustMarkStatusResponse(status, false)).isEmpty());
  }

  @Test
  void statusForOtherTrustMarkIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    final String otherTrustMark = this.trustMark().build();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(otherTrustMark)).isEmpty());
  }

  @Test
  void trustMarkForOtherSubjectIsRejected() throws Exception {
    final String trustMark = this.trustMark().subject("https://example.com/other").build();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(trustMark)).isEmpty());
  }

  @Test
  void trustMarkWithWrongTypIsRejected() throws Exception {
    final String trustMark = this.trustMark().typ("jwt").build();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(trustMark)).isEmpty());
  }

  @Test
  void expiredTrustMarkIsRejected() throws Exception {
    final String trustMark = this.trustMark().expiration(Instant.now().minusSeconds(60)).build();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(trustMark)).isEmpty());
  }

  @Test
  void trustMarkNotSignedByIssuerIsRejected() throws Exception {
    final String trustMark = this.trustMark().signingKey(this.leafKey).build();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(trustMark)).isEmpty());
  }

  @Test
  void trustMarkFromUnresolvableIssuerIsRejected() throws Exception {
    final String trustMark = this.trustMark().build();
    this.issuerKeys = issuer -> Optional.empty();
    Assertions.assertTrue(this.collect(trustMark, this.activeStatus(trustMark)).isEmpty());
  }

  @Test
  void superiorStatementWithoutTrustMarksIsHandled() throws Exception {
    final String trustMark = this.trustMark().build();
    final List<SignedJWT> statements = List.of(
        this.leafStatement(trustMark, "trust_mark_type"),
        this.otherStatement(),
        // Position 2 in the chain is read as the statement issued for the subject
        this.subordinateStatementForSubject(),
        this.trustAnchor(null));
    Assertions.assertEquals(1, this.collect(statements, this.activeStatus(trustMark)).size());
  }

  @Test
  void draftEntryAndTrustMarkWithIdIsCollected() throws Exception {
    final String trustMark = this.trustMark().typeClaim("id").build();
    final List<SignedJWT> statements = List.of(
        this.leafStatement(trustMark, "id"), this.otherStatement(), this.trustAnchor(null));
    Assertions.assertEquals(1, this.collect(statements, this.activeStatus(trustMark)).size());
  }

  @Test
  void delegatedTrustMarkIsCollected() throws Exception {
    final JWK ownerKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    final String delegation = this.delegation(ownerKey, ISSUER, Instant.now().plusSeconds(3600));
    Assertions.assertEquals(1, this.collectWithOwner(ownerKey, delegation).size());
  }

  @Test
  void ownedTrustMarkWithoutDelegationIsRejected() throws Exception {
    final JWK ownerKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    Assertions.assertTrue(this.collectWithOwner(ownerKey, null).isEmpty());
  }

  @Test
  void delegationNotSignedByOwnerIsRejected() throws Exception {
    final JWK ownerKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    final JWK otherKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    final String delegation = this.delegation(otherKey, ISSUER, Instant.now().plusSeconds(3600));
    Assertions.assertTrue(this.collectWithOwner(ownerKey, delegation).isEmpty());
  }

  @Test
  void delegationForOtherIssuerIsRejected() throws Exception {
    final JWK ownerKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    final String delegation =
        this.delegation(ownerKey, "https://example.com/other-issuer", Instant.now().plusSeconds(3600));
    Assertions.assertTrue(this.collectWithOwner(ownerKey, delegation).isEmpty());
  }

  @Test
  void expiredDelegationIsRejected() throws Exception {
    final JWK ownerKey = new RSAKeyGenerator(2048).keyID("owner-key").generate();
    final String delegation = this.delegation(ownerKey, ISSUER, Instant.now().minusSeconds(60));
    Assertions.assertTrue(this.collectWithOwner(ownerKey, delegation).isEmpty());
  }

  private List<TrustMarkEntry> collect(final String trustMark, final TrustMarkStatusResponse status)
      throws Exception {
    final List<SignedJWT> statements = List.of(
        this.leafStatement(trustMark, "trust_mark_type"), this.otherStatement(), this.trustAnchor(null));
    return this.collect(statements, status);
  }

  private List<TrustMarkEntry> collect(final List<SignedJWT> statements, final TrustMarkStatusResponse status)
      throws Exception {
    final Map<String, TrustMarkStatusResponse> statuses = new HashMap<>();
    if (status != null) {
      statuses.put(TRUST_MARK_TYPE, status);
    }
    final ScrapedEntity leafEntity = ScrapedEntity.builder()
        .entityID(new EntityID(SUBJECT))
        .trustMarkStatuses(statuses)
        .build();
    final ResolverTrustChain chain = new ResolverTrustChain(new LinkedHashSet<>(statements), leafEntity);
    return TrustMarkCollector.collectSubjectTrustMarks(chain, this.issuerKeys);
  }

  private List<TrustMarkEntry> collectWithOwner(final JWK ownerKey, final String delegation) throws Exception {
    final String trustMark = this.trustMark().delegation(delegation).build();
    final List<SignedJWT> statements = List.of(
        this.leafStatement(trustMark, "trust_mark_type"), this.otherStatement(), this.trustAnchor(ownerKey));
    return this.collect(statements, this.activeStatus(trustMark));
  }

  private TrustMarkBuilder trustMark() {
    return new TrustMarkBuilder(this.issuerKey);
  }

  private TrustMarkStatusResponse activeStatus(final String trustMark) throws Exception {
    return new TrustMarkStatusResponse(this.status(this.issuerKey, STATUS_TYPE, ISSUER, trustMark, "active"), false);
  }

  private SignedJWT status(final JWK key, final String typ, final String issuer, final String trustMark,
      final String status) throws Exception {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .issueTime(new Date())
        .claim("trust_mark", trustMark)
        .claim("status", status)
        .build();
    return sign(key, typ, claims);
  }

  private String delegation(final JWK ownerKey, final String subject, final Instant expiration) throws Exception {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(OWNER)
        .subject(subject)
        .claim("trust_mark_type", TRUST_MARK_TYPE)
        .issueTime(Date.from(Instant.now().minusSeconds(7200)))
        .expirationTime(Date.from(expiration))
        .build();
    return sign(ownerKey, "trust-mark-delegation+jwt", claims).serialize();
  }

  private SignedJWT leafStatement(final String trustMark, final String typeMember) throws Exception {
    final JSONObject trustMarkEntry = new JSONObject();
    trustMarkEntry.put(typeMember, TRUST_MARK_TYPE);
    trustMarkEntry.put("trust_mark", trustMark);
    final JSONArray trustMarks = new JSONArray();
    trustMarks.add(trustMarkEntry);

    final JWTClaimsSet claims = this.statementClaims(SUBJECT, SUBJECT, this.leafKey)
        .claim("trust_marks", trustMarks)
        .build();
    return sign(this.leafKey, "entity-statement+jwt", claims);
  }

  private SignedJWT otherStatement() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("superior-key").generate();
    return sign(key, "entity-statement+jwt",
        this.statementClaims("https://example.com/intermediate", "https://example.com/other-entity", key).build());
  }

  private SignedJWT subordinateStatementForSubject() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("superior-key").generate();
    return sign(key, "entity-statement+jwt",
        this.statementClaims("https://example.com/intermediate", SUBJECT, key).build());
  }

  private SignedJWT trustAnchor(final JWK ownerKey) throws Exception {
    final JWK taKey = new RSAKeyGenerator(2048).keyID("ta-key").generate();
    final JSONArray issuers = new JSONArray();
    issuers.add(ISSUER);
    final JSONObject trustMarkIssuers = new JSONObject();
    trustMarkIssuers.put(TRUST_MARK_TYPE, issuers);

    final JWTClaimsSet.Builder builder =
        this.statementClaims("https://example.com/ta", "https://example.com/ta", taKey)
            .claim("trust_mark_issuers", trustMarkIssuers);
    if (ownerKey != null) {
      final JSONObject owner = new JSONObject();
      owner.put("sub", OWNER);
      owner.put("jwks", new JSONObject(new JWKSet(ownerKey.toPublicJWK()).toJSONObject()));
      final JSONObject trustMarkOwners = new JSONObject();
      trustMarkOwners.put(TRUST_MARK_TYPE, owner);
      builder.claim("trust_mark_owners", trustMarkOwners);
    }
    return sign(taKey, "entity-statement+jwt", builder.build());
  }

  private JWTClaimsSet.Builder statementClaims(final String issuer, final String subject, final JWK key) {
    final JSONObject federationEntity = new JSONObject();
    federationEntity.put("organization_name", "Test");
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntity);
    return new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("jwks", new JSONObject(new JWKSet(key.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata);
  }

  private static SignedJWT sign(final JWK key, final String typ, final JWTClaimsSet claims) throws Exception {
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType(typ))
        .keyID(key.getKeyID())
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt;
  }

  /**
   * Builds trust mark JWTs that are valid unless a value is overridden.
   */
  private static class TrustMarkBuilder {
    private JWK signingKey;
    private String typ = TRUST_MARK_JWT_TYPE;
    private String typeClaim = "trust_mark_type";
    private String subject = SUBJECT;
    private Instant expiration = Instant.now().plusSeconds(3600);
    private String delegation;

    TrustMarkBuilder(final JWK signingKey) {
      this.signingKey = signingKey;
    }

    TrustMarkBuilder signingKey(final JWK signingKey) {
      this.signingKey = signingKey;
      return this;
    }

    TrustMarkBuilder typ(final String typ) {
      this.typ = typ;
      return this;
    }

    TrustMarkBuilder typeClaim(final String typeClaim) {
      this.typeClaim = typeClaim;
      return this;
    }

    TrustMarkBuilder subject(final String subject) {
      this.subject = subject;
      return this;
    }

    TrustMarkBuilder expiration(final Instant expiration) {
      this.expiration = expiration;
      return this;
    }

    TrustMarkBuilder delegation(final String delegation) {
      this.delegation = delegation;
      return this;
    }

    String build() throws Exception {
      final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
          .issuer(ISSUER)
          .subject(this.subject)
          .claim(this.typeClaim, TRUST_MARK_TYPE)
          .issueTime(new Date())
          .expirationTime(Date.from(this.expiration))
          // Unique per trust mark, so that two trust marks never serialize the same
          .jwtID(UUID.randomUUID().toString());
      if (this.delegation != null) {
        claims.claim("delegation", this.delegation);
      }
      return sign(this.signingKey, this.typ, claims.build()).serialize();
    }
  }
}
