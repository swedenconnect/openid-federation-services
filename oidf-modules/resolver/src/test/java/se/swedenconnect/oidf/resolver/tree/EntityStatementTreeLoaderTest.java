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
package se.swedenconnect.oidf.resolver.tree;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.VersionedInMemoryCache;
import se.swedenconnect.oidf.common.entity.tree.scraping.FetchError;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;
import se.swedenconnect.oidf.common.entity.tree.scraping.WrongJwkKidException;
import se.swedenconnect.oidf.resolver.FederationClientMockFactory;
import se.swedenconnect.oidf.resolver.TestEntitiesFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.IM_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.SAML_SP_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.TA_ID;

/**
 * Tests for {@link EntityStatementTreeLoader} when parts of an entity cannot be fetched during a load.
 */
class EntityStatementTreeLoaderTest {

  private TestEntitiesFactory entities;
  private VersionedInMemoryCache cache;
  private Tree<ScrapedEntity> tree;
  private FederationClient client;
  private final List<String> errors = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    this.entities = new TestEntitiesFactory();
    this.cache = new VersionedInMemoryCache();
    this.tree = new Tree<>(this.cache);
    this.client = FederationClientMockFactory.create(this.entities);
  }

  @Test
  void failedFetchKeepsPreviousData() {
    this.load(this.client, 1L);
    final Instant lastSeen = this.data(LEAF_ID, 1L).getLastSeen();
    assertNotNull(lastSeen);

    this.load(this.failingEntityConfiguration(LEAF_ID), 2L);

    final ScrapedEntity leaf = this.data(LEAF_ID, 2L);
    assertNotNull(leaf, "Leaf should be kept in the new snapshot");
    assertEquals(lastSeen, leaf.getLastSeen());
    assertEquals(FetchError.Kind.INVALID_RESPONSE, leaf.getLastError().kind());
    assertEquals(this.entities.leafEC.serialize(), leaf.getEntityStatement().serialize());
    assertNull(this.data(LEAF_ID, 1L).getLastError(), "Previous snapshot should not be changed");
    assertEquals(List.of(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"), this.errors);
  }

  @Test
  void lastSeenIsKeptUntilFetchSucceeds() {
    this.load(this.client, 1L);
    final Instant lastSeen = this.data(LEAF_ID, 1L).getLastSeen();
    this.load(this.failingEntityConfiguration(LEAF_ID), 2L);
    this.load(this.failingEntityConfiguration(LEAF_ID), 3L);
    assertEquals(lastSeen, this.data(LEAF_ID, 3L).getLastSeen());

    this.load(this.client, 4L);
    assertNull(this.data(LEAF_ID, 4L).getLastError());
    assertTrue(this.data(LEAF_ID, 4L).getLastSeen().isAfter(lastSeen));
  }

  @Test
  void failedFetchWithoutPreviousDataDropsEntity() {
    this.load(this.failingEntityConfiguration(LEAF_ID), 1L);

    assertNull(this.data(LEAF_ID, 1L));
    assertNotNull(this.data(SAML_SP_ID, 1L));
    assertEquals(List.of(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"), this.errors);
  }

  @Test
  void expiredPreviousDataIsNotUsed() throws Exception {
    this.load(this.client, 1L);
    final ScrapedEntity expired = ScrapedEntity.builder()
        .entityID(new EntityID(LEAF_ID))
        .entityStatement(expiredStatement(LEAF_ID, LEAF_ID))
        .build();
    this.cache.setData(LEAF_ID, expired, 1L);

    this.load(this.failingEntityConfiguration(LEAF_ID), 2L);

    assertNull(this.data(LEAF_ID, 2L));
    assertTrue(this.errors.contains(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"));
  }

  @Test
  void fetchedExpiredEntityConfigurationIsKept() throws Exception {
    final SignedJWT expired = expiredStatement(LEAF_ID, LEAF_ID);
    final FederationClient expiredClient = this.spy();
    doReturn(expired).when(expiredClient).entityConfiguration(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().entityID().getValue())));

    this.load(expiredClient, 1L);

    assertEquals(expired.serialize(), this.data(LEAF_ID, 1L).getEntityStatement().serialize());
    assertEquals(FetchError.expired(), this.data(LEAF_ID, 1L).getLastError());
    assertNotNull(this.data(LEAF_ID, 1L).getLastSeen());
    assertTrue(this.errors.isEmpty());
  }

  @Test
  void fetchedExpiredEntityConfigurationReplacesPreviousData() throws Exception {
    this.load(this.client, 1L);
    final SignedJWT expired = expiredStatement(LEAF_ID, LEAF_ID);
    final FederationClient expiredClient = this.spy();
    doReturn(expired).when(expiredClient).entityConfiguration(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().entityID().getValue())));

    this.load(expiredClient, 2L);

    assertEquals(expired.serialize(), this.data(LEAF_ID, 2L).getEntityStatement().serialize());
    assertEquals(FetchError.expired(), this.data(LEAF_ID, 2L).getLastError());
  }

  @Test
  void fetchedExpiredIntermediateIsTraversed() throws Exception {
    final FederationClient expiredClient = this.spy();
    doReturn(expiredCopy(this.entities.imEC)).when(expiredClient).entityConfiguration(
        argThat(r -> r != null && IM_ID.equals(r.parameters().entityID().getValue())));

    this.load(expiredClient, 1L);

    assertEquals(FetchError.expired(), this.data(IM_ID, 1L).getLastError());
    assertNotNull(this.data(LEAF_ID, 1L));
    assertNotNull(this.data(SAML_SP_ID, 1L));
  }

  @Test
  void expiredPreviousIntermediateIsKept() throws Exception {
    this.load(this.client, 1L);
    this.data(IM_ID, 1L).setEntityStatement(expiredCopy(this.entities.imEC));

    this.load(this.failingEntityConfiguration(IM_ID), 2L);

    assertNotNull(this.data(IM_ID, 2L));
    assertEquals(FetchError.Kind.INVALID_RESPONSE, this.data(IM_ID, 2L).getLastError().kind());
    assertNotNull(this.data(LEAF_ID, 2L));
    assertNotNull(this.data(SAML_SP_ID, 2L));
    assertEquals(List.of(IM_ID + ":FETCH_ENTITY_CONFIGURATION"), this.errors);
  }

  @Test
  void wrongKidIsNotReplacedWithPreviousData() {
    this.load(this.client, 1L);
    final FederationClient wrongKid = this.spy();
    doThrow(new WrongJwkKidException(LEAF_ID, "kid", Set.of("other"))).when(wrongKid).entityConfiguration(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().entityID().getValue())));

    this.load(wrongKid, 2L);

    assertNull(this.data(LEAF_ID, 2L));
  }

  @Test
  void failedTrustAnchorKeepsPreviousData() {
    this.load(this.client, 1L);

    this.load(this.failingEntityConfiguration(TA_ID), 2L);

    final ScrapedEntity ta = this.data(TA_ID, 2L);
    assertNotNull(ta);
    assertNotNull(ta.getLastError());
    assertNotNull(this.data(LEAF_ID, 2L), "Rebuild should continue below the trust anchor");
    assertEquals(List.of(TA_ID + ":RESOLVE_ROOT"), this.errors);
  }

  @Test
  void failedTrustAnchorWithoutPreviousDataFailsLoad() {
    assertThrows(IllegalStateException.class, () -> this.load(this.failingEntityConfiguration(TA_ID), 1L));
  }

  @Test
  void failedListingKeepsPreviousListing() {
    this.load(this.client, 1L);
    final FederationClient failing = this.spy();
    doThrow(new RuntimeException("Connection refused")).when(failing).subordinateListing(argThat(r ->
        r != null && ((String) r.federationEntityMetadata().get("federation_list_endpoint")).startsWith(IM_ID)));

    this.load(failing, 2L);

    assertEquals(List.of(LEAF_ID, SAML_SP_ID), this.data(IM_ID, 2L).getIntermediate().listing());
    assertNotNull(this.data(LEAF_ID, 2L));
    assertNotNull(this.data(SAML_SP_ID, 2L));
    assertEquals(List.of(IM_ID + ":SUBORDINATE_LISTING"), this.errors);
  }

  @Test
  void failedListingWithoutPreviousDataDropsIntermediate() {
    final FederationClient failing = this.spy();
    doThrow(new RuntimeException("Connection refused")).when(failing).subordinateListing(argThat(r ->
        r != null && ((String) r.federationEntityMetadata().get("federation_list_endpoint")).startsWith(IM_ID)));

    this.load(failing, 1L);

    assertNull(this.data(IM_ID, 1L));
    assertNull(this.data(LEAF_ID, 1L));
  }

  @Test
  void failedStatementOnlyAffectsThatSubordinate() {
    this.load(this.failingStatement(LEAF_ID), 1L);

    assertNull(this.data(LEAF_ID, 1L));
    assertNotNull(this.data(SAML_SP_ID, 1L));
    assertEquals(List.of(LEAF_ID + ":FETCH_SUBORDINATE_STATEMENT"), this.errors);
  }

  @Test
  void failedStatementKeepsPreviousStatement() {
    this.load(this.client, 1L);

    this.load(this.failingStatement(LEAF_ID), 2L);

    assertEquals(this.entities.imLeafSub.serialize(),
        this.data(IM_ID, 2L).getIntermediate().subordinates().get(LEAF_ID).serialize());
    assertNotNull(this.data(LEAF_ID, 2L));
  }

  @Test
  void expiredPreviousStatementIsNotUsed() throws Exception {
    this.load(this.client, 1L);
    final ScrapedEntity im = this.data(IM_ID, 1L);
    final Map<String, SignedJWT> statements = new HashMap<>(im.getIntermediate().subordinates());
    statements.put(LEAF_ID, expiredStatement(IM_ID, LEAF_ID));
    im.setIntermediate(new ScrapedIntermediate(statements));

    this.load(this.failingStatement(LEAF_ID), 2L);

    assertNull(this.data(IM_ID, 2L).getIntermediate().subordinates().get(LEAF_ID));
    assertNull(this.data(LEAF_ID, 2L));
    assertNotNull(this.data(SAML_SP_ID, 2L));
  }

  @Test
  void entityNoLongerListedIsRemoved() {
    this.load(this.client, 1L);
    final FederationClient shorter = this.spy();
    doReturn(List.of(SAML_SP_ID)).when(shorter).subordinateListing(argThat(r ->
        r != null && ((String) r.federationEntityMetadata().get("federation_list_endpoint")).startsWith(IM_ID)));

    this.load(shorter, 2L);

    assertNull(this.data(LEAF_ID, 2L));
    assertNotNull(this.data(SAML_SP_ID, 2L));
  }

  private void load(final FederationClient federationClient, final long version) {
    new EntityStatementTreeLoader(federationClient, new DFSExecution(), (key, stepName) -> {
      this.errors.add(key.getKey() + ":" + stepName.name());
      return new AtomicIntegerErrorContext();
    }, new EcLocationValidator(false)).resolveTree(TA_ID, this.tree, version);
    this.cache.useNextVersion();
  }

  private FederationClient spy() {
    return Mockito.mock(FederationClient.class, AdditionalAnswers.delegatesTo(this.client));
  }

  private FederationClient failingEntityConfiguration(final String entityId) {
    final FederationClient failing = this.spy();
    doThrow(new RuntimeException("Connection refused")).when(failing).entityConfiguration(
        argThat(r -> r != null && entityId.equals(r.parameters().entityID().getValue())));
    return failing;
  }

  private FederationClient failingStatement(final String subject) {
    final FederationClient failing = this.spy();
    doThrow(new RuntimeException("Connection refused")).when(failing).fetch(
        argThat(r -> r != null && subject.equals(r.parameters().subject())));
    return failing;
  }

  private ScrapedEntity data(final String entityId, final long version) {
    return this.cache.getData(new NodeKey(entityId).getKey(), version);
  }

  private static SignedJWT expiredCopy(final SignedJWT statement) throws Exception {
    final RSAKey key = new RSAKeyGenerator(2048).generate();
    final Instant now = Instant.now();
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
        new JWTClaimsSet.Builder(statement.getJWTClaimsSet())
            .issueTime(Date.from(now.minus(Duration.ofDays(2))))
            .expirationTime(Date.from(now.minus(Duration.ofDays(1))))
            .build());
    jwt.sign(new RSASSASigner(key));
    return jwt;
  }

  private static SignedJWT expiredStatement(final String issuer, final String subject) throws Exception {
    final RSAKey key = new RSAKeyGenerator(2048).keyID("expired").generate();
    final Instant now = Instant.now();
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .issueTime(Date.from(now.minus(Duration.ofDays(2))))
            .expirationTime(Date.from(now.minus(Duration.ofDays(1))))
            .build());
    jwt.sign(new RSASSASigner(key));
    return jwt;
  }
}
