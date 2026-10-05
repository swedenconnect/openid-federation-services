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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for the {@code trust_mark_type} filter of {@link ValidatingResolver#discovery(DiscoveryRequest)}.
 */
class ValidatingResolverDiscoveryTest {

  private static final String TA = "https://ta.example.com";
  private static final String LEAF = "https://leaf.example.com";
  private static final String ISSUER = "https://tmi.example.com";
  private static final String TYPE = "https://example.com/tm/certified";

  private RSAKey issuerKey;
  private RSAKey entityKey;
  private EntityStatementTree tree;
  private ValidatingResolver resolver;

  @BeforeEach
  void setUp() throws Exception {
    this.issuerKey = new RSAKeyGenerator(2048).keyID("issuer").generate();
    this.entityKey = new RSAKeyGenerator(2048).keyID("entity").generate();
    this.tree = Mockito.mock(EntityStatementTree.class);
    Mockito.when(this.tree.discovery(Mockito.any())).thenReturn(List.of(LEAF));
    final SignedJWT issuerConfiguration =
        this.entityConfiguration(ISSUER, List.of(), new JWKSet(this.issuerKey.toPublicJWK()));
    Mockito.when(this.tree.getTrustChainViaAuthorityHints(Mockito.argThat(r -> r != null && ISSUER.equals(r.subject()))))
        .thenReturn(Optional.of(new ResolverTrustChain(new LinkedHashSet<>(List.of(issuerConfiguration)), null)));
    this.resolver = new ValidatingResolver(new ResolverProperties(TA, Duration.ofHours(1), null, "https://resolver"),
        Mockito.mock(ChainValidator.class), this.tree, new MetadataProcessor(),
        Mockito.mock(ResolverResponseFactory.class));
  }

  @Test
  void validTrustMarkIsFound() throws Exception {
    this.leafWithTrustMark(TYPE, this.trustMark(TYPE, this.issuerKey, Instant.now().plusSeconds(600)));

    Assertions.assertEquals(List.of(LEAF), this.discover());
  }

  @Test
  void expiredTrustMarkIsNotCounted() throws Exception {
    this.leafWithTrustMark(TYPE, this.trustMark(TYPE, this.issuerKey, Instant.now().minusSeconds(60)));

    Assertions.assertEquals(List.of(), this.discover());
  }

  @Test
  void trustMarkNotSignedByIssuerIsNotCounted() throws Exception {
    final RSAKey otherKey = new RSAKeyGenerator(2048).keyID("issuer").generate();
    this.leafWithTrustMark(TYPE, this.trustMark(TYPE, otherKey, Instant.now().plusSeconds(600)));

    Assertions.assertEquals(List.of(), this.discover());
  }

  @Test
  void trustMarkOfOtherTypeThanEntryIsNotCounted() throws Exception {
    this.leafWithTrustMark(TYPE,
        this.trustMark("https://example.com/tm/other", this.issuerKey, Instant.now().plusSeconds(600)));

    Assertions.assertEquals(List.of(), this.discover());
  }

  @Test
  void withoutTrustMarkFilterNoTrustMarkIsChecked() {
    Assertions.assertEquals(List.of(LEAF),
        this.resolver.discovery(new DiscoveryRequest(TA, null, null)).supportedEntities());
    Mockito.verify(this.tree, Mockito.never()).getTrustChainViaAuthorityHints(Mockito.any());
  }

  private List<String> discover() {
    return this.resolver.discovery(new DiscoveryRequest(TA, null, List.of(TYPE))).supportedEntities();
  }

  private void leafWithTrustMark(final String entryType, final SignedJWT trustMark) throws Exception {
    final SignedJWT leafConfiguration = this.entityConfiguration(LEAF,
        List.of(new JSONObject(Map.of("trust_mark_type", entryType, "trust_mark", trustMark.serialize()))),
        new JWKSet(this.entityKey.toPublicJWK()));
    final Map<String, TrustMarkStatusResponse> statuses = new HashMap<>();
    statuses.put(trustMark.serialize(), TrustMarkStatusResponse.noStatusEndpoint());
    final ScrapedEntity leaf = ScrapedEntity.builder()
        .entityID(new EntityID(LEAF))
        .entityStatement(leafConfiguration)
        .trustMarkStatuses(statuses)
        .build();
    final SignedJWT taConfiguration =
        this.entityConfiguration(TA, List.of(), new JWKSet(this.entityKey.toPublicJWK()));
    Mockito.when(this.tree.getTrustChainViaAuthorityHints(Mockito.argThat(r -> r != null && LEAF.equals(r.subject()))))
        .thenReturn(Optional.of(new ResolverTrustChain(
            new LinkedHashSet<>(List.of(leafConfiguration, taConfiguration)), leaf)));
  }

  private SignedJWT trustMark(final String type, final RSAKey signingKey, final Instant expiration)
      throws Exception {
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("trust-mark+jwt"))
        .keyID(signingKey.getKeyID())
        .build(),
        new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .subject(LEAF)
            .claim("trust_mark_type", type)
            .issueTime(new Date())
            .expirationTime(Date.from(expiration))
            .build());
    jwt.sign(new RSASSASigner(signingKey));
    return jwt;
  }

  private SignedJWT entityConfiguration(final String id, final List<JSONObject> trustMarks, final JWKSet jwks)
      throws Exception {
    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
        .issuer(id)
        .subject(id)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("jwks", new JSONObject(jwks.toJSONObject()));
    if (!trustMarks.isEmpty()) {
      claims.claim("trust_marks", trustMarks);
    }
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
        .type(new JOSEObjectType("entity-statement+jwt"))
        .keyID(this.entityKey.getKeyID())
        .build(), claims.build());
    jwt.sign(new RSASSASigner(this.entityKey));
    return jwt;
  }
}
