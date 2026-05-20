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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TrustMarkCollectorStatusTest {

  private static final String TRUST_MARK_TYPE = "https://example.com/trustmark/type1";
  private static final String SUBJECT = "https://example.com/subject";
  private static final String ISSUER = "https://example.com/issuer";

  @Test
  void statusStoreFiltersInactiveTrustMarks() throws Exception {
    final TrustMarkCollector collector = new TrustMarkCollector();

    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    final String trustMarkJwt = buildTrustMarkJwt(key);

    final EntityStatement leafStatement = buildEntityStatementWithTrustMark(key, trustMarkJwt);
    final EntityStatement superiorStatement = buildEntityStatementWithoutTrustMarks();
    final EntityStatement trustAnchor = buildTrustAnchorStatement(key);

    final ScrapedEntity leafEntity = ScrapedEntity.builder()
        .entityID(new EntityID(SUBJECT))
        .trustMarkStatuses(Map.of(TRUST_MARK_TYPE, new TrustMarkStatusResponse(buildStatusJwt("inactive"), false)))
        .build();
    final Set<EntityStatement> statements = new LinkedHashSet<>(List.of(leafStatement, superiorStatement, trustAnchor));
    final ResolverTrustChain chain = new ResolverTrustChain(statements, leafEntity);
    final List<TrustMarkEntry> result = TrustMarkCollector.collectSubjectTrustMarks(chain);

    Assertions.assertTrue(result.isEmpty(),
        "Trust mark with inactive status should be filtered out");
  }

  @Test
  void statusStoreAllowsActiveTrustMarks() throws Exception {
    final TrustMarkCollector collector = new TrustMarkCollector();

    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    final String trustMarkJwt = buildTrustMarkJwt(key);

    final EntityStatement leafStatement = buildEntityStatementWithTrustMark(key, trustMarkJwt);
    final EntityStatement superiorStatement = buildEntityStatementWithoutTrustMarks();
    final EntityStatement trustAnchor = buildTrustAnchorStatement(key);

    final ScrapedEntity leafEntity = ScrapedEntity.builder()
        .entityID(new EntityID(SUBJECT))
        .trustMarkStatuses(Map.of(TRUST_MARK_TYPE, new TrustMarkStatusResponse(buildStatusJwt("active"), false)))
        .build();
    final Set<EntityStatement> statements = new LinkedHashSet<>(List.of(leafStatement, superiorStatement, trustAnchor));
    final ResolverTrustChain chain = new ResolverTrustChain(statements, leafEntity);
    final List<TrustMarkEntry> result = TrustMarkCollector.collectSubjectTrustMarks(chain);

    Assertions.assertEquals(1, result.size(),
        "Trust mark with active status should be included");
  }

  @Test
  void missingStatusInStoreMeansInclude() throws Exception {
    final TrustMarkCollector collector = new TrustMarkCollector();

    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    final String trustMarkJwt = buildTrustMarkJwt(key);

    final EntityStatement leafStatement = buildEntityStatementWithTrustMark(key, trustMarkJwt);
    final EntityStatement superiorStatement = buildEntityStatementWithoutTrustMarks();
    final EntityStatement trustAnchor = buildTrustAnchorStatement(key);

    final ScrapedEntity leafEntity = ScrapedEntity.builder()
        .entityID(new EntityID(SUBJECT))
        .trustMarkStatuses(Map.of())
        .build();
    final Set<EntityStatement> statements = new LinkedHashSet<>(List.of(leafStatement, superiorStatement, trustAnchor));
    final ResolverTrustChain chain = new ResolverTrustChain(statements, leafEntity);
    final List<TrustMarkEntry> result = TrustMarkCollector.collectSubjectTrustMarks(chain);

    Assertions.assertEquals(0, result.size(),
        "Trust mark with no recorded status should be excluded (orElse(false))");
  }

  private String buildTrustMarkJwt(final JWK key) throws Exception {
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("trust-mark+jwt"))
        .keyID(key.getKeyID())
        .build();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(ISSUER)
        .subject(SUBJECT)
        .claim("trust_mark_type", TRUST_MARK_TYPE)
        .issueTime(Date.from(Instant.now()))
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt.serialize();
  }

  private EntityStatement buildEntityStatementWithTrustMark(final JWK key, final String trustMarkJwt)
      throws Exception {
    final JSONObject trustMarkEntry = new JSONObject();
    trustMarkEntry.put("id", TRUST_MARK_TYPE);
    trustMarkEntry.put("trust_mark", trustMarkJwt);

    final JSONArray trustMarks = new JSONArray();
    trustMarks.add(trustMarkEntry);

    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(key.getKeyID())
        .build();

    final JWKSet jwkSet = new JWKSet(key.toPublicJWK());
    final JSONObject federationEntityMetadata = new JSONObject();
    federationEntityMetadata.put("organization_name", "Test");
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntityMetadata);
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(SUBJECT)
        .subject(SUBJECT)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("trust_marks", trustMarks)
        .claim("jwks", new JSONObject(jwkSet.toJSONObject()))
        .claim("metadata", metadata)
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return EntityStatement.parse(jwt.serialize());
  }

  private EntityStatement buildEntityStatementWithoutTrustMarks() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("superior-key").generate();
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(key.getKeyID())
        .build();

    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer("https://example.com/intermediate")
        .subject("https://example.com/other-entity")
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("jwks", new JSONObject(new JWKSet(key.toPublicJWK()).toJSONObject()))
        .claim("metadata", new JSONObject())
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return EntityStatement.parse(jwt.serialize());
  }

  private SignedJWT buildStatusJwt(final String status) throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("status-key").generate();
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(ISSUER)
        .subject(SUBJECT)
        .claim("status", status)
        .build();
    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(key.toRSAKey()));
    return jwt;
  }

  private EntityStatement buildTrustAnchorStatement(final JWK ownerKey) throws Exception {
    final JWK taKey = new RSAKeyGenerator(2048).keyID("ta-key").generate();
    final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(taKey.getKeyID())
        .build();

    final JSONObject jwksObj = new JSONObject(new JWKSet(ownerKey.toPublicJWK()).toJSONObject());
    final JSONObject ownerEntry = new JSONObject();
    ownerEntry.put("jwks", jwksObj);

    final JSONObject trustMarkOwners = new JSONObject();
    trustMarkOwners.put(TRUST_MARK_TYPE, ownerEntry);

    final JSONArray issuerArray = new JSONArray();
    final JSONObject issuerEntry = new JSONObject();
    issuerEntry.put("value", ISSUER);
    issuerArray.add(issuerEntry);

    final JSONObject trustMarkIssuers = new JSONObject();
    trustMarkIssuers.put(TRUST_MARK_TYPE, issuerArray);

    final JSONObject federationEntityMetadata = new JSONObject();
    federationEntityMetadata.put("organization_name", "TA");
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", federationEntityMetadata);
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer("https://example.com/ta")
        .subject("https://example.com/ta")
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("trust_mark_owners", trustMarkOwners)
        .claim("trust_mark_issuers", trustMarkIssuers)
        .claim("jwks", new JSONObject(new JWKSet(taKey.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata)
        .build();

    final SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(taKey.toRSAKey()));
    return EntityStatement.parse(jwt.serialize());
  }
}
