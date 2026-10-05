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
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.FederationClientMockFactory;
import se.swedenconnect.oidf.resolver.TestEntitiesFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.TA_ID;

/**
 * Tests for {@link EntityStatementTreeLoader} when an entity cannot be fetched during a load.
 */
class EntityStatementTreeLoaderTest {

  private TestEntitiesFactory entities;
  private VersionedInMemoryCache cache;
  private Tree<ScrapedEntity> tree;
  private FederationClient client;
  private FederationClient failingClient;
  private final List<String> errors = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    this.entities = new TestEntitiesFactory();
    this.cache = new VersionedInMemoryCache();
    this.tree = new Tree<>(this.cache);
    this.client = FederationClientMockFactory.create(this.entities);
    this.failingClient = Mockito.mock(FederationClient.class, AdditionalAnswers.delegatesTo(this.client));
    doThrow(new RuntimeException("Connection refused")).when(this.failingClient).entityConfiguration(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().entityID().getValue())));
  }

  @Test
  void failedFetchKeepsPreviousData() {
    this.load(this.client, 1L);
    this.load(this.failingClient, 2L);

    final ScrapedEntity leaf = this.leaf(2L);
    assertNotNull(leaf, "Leaf should be kept in the new snapshot");
    assertNotNull(leaf.getScrapeFailedAt());
    assertEquals(this.entities.leafEC.serialize(), leaf.getEntityStatement().serialize());
    assertNull(this.leaf(1L).getScrapeFailedAt(), "Previous snapshot should not be changed");
    assertEquals(List.of(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"), this.errors);
  }

  @Test
  void failureTimeIsKeptUntilFetchSucceeds() {
    this.load(this.client, 1L);
    this.load(this.failingClient, 2L);
    final Instant failedAt = this.leaf(2L).getScrapeFailedAt();
    this.load(this.failingClient, 3L);
    assertEquals(failedAt, this.leaf(3L).getScrapeFailedAt());

    this.load(this.client, 4L);
    assertNull(this.leaf(4L).getScrapeFailedAt());
  }

  @Test
  void failedFetchWithoutPreviousDataDropsEntity() {
    this.load(this.failingClient, 1L);

    assertNull(this.leaf(1L));
    assertEquals(List.of(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"), this.errors);
  }

  @Test
  void expiredPreviousDataIsNotUsed() throws Exception {
    this.load(this.client, 1L);
    final ScrapedEntity expired = ScrapedEntity.builder()
        .entityID(new EntityID(LEAF_ID))
        .entityStatement(expiredStatement(LEAF_ID))
        .build();
    this.cache.setData(LEAF_ID, expired, 1L);

    this.load(this.failingClient, 2L);

    assertNull(this.leaf(2L));
    assertTrue(this.errors.contains(LEAF_ID + ":FETCH_ENTITY_CONFIGURATION"));
  }

  private void load(final FederationClient federationClient, final long version) {
    new EntityStatementTreeLoader(federationClient, new DFSExecution(), (key, stepName) -> {
      this.errors.add(key.getKey() + ":" + stepName.name());
      return new AtomicIntegerErrorContext();
    }, new EcLocationValidator(false)).resolveTree(TA_ID, this.tree, version);
    this.cache.useNextVersion();
  }

  private ScrapedEntity leaf(final long version) {
    return this.cache.getData(new NodeKey(LEAF_ID).getKey(), version);
  }

  private static SignedJWT expiredStatement(final String entityId) throws Exception {
    final RSAKey key = new RSAKeyGenerator(2048).keyID("expired").generate();
    final Instant now = Instant.now();
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
        new JWTClaimsSet.Builder()
            .issuer(entityId)
            .subject(entityId)
            .issueTime(Date.from(now.minus(Duration.ofDays(2))))
            .expirationTime(Date.from(now.minus(Duration.ofDays(1))))
            .build());
    jwt.sign(new RSASSASigner(key));
    return jwt;
  }
}
