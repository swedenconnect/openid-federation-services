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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.VersionedInMemoryCache;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.FederationClientMockFactory;
import se.swedenconnect.oidf.resolver.TestEntitiesFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.TA_ID;

/**
 * Tests for how {@link EntityStatementTreeLoader} handles the {@code ec_location} claim of Subordinate Statements.
 */
class EntityStatementTreeLoaderEcLocationTest {

  private TestEntitiesFactory entities;
  private VersionedInMemoryCache cache;
  private FederationClient client;
  private final List<String> errors = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    this.entities = new TestEntitiesFactory();
    this.cache = new VersionedInMemoryCache();
    this.client = Mockito.mock(FederationClient.class,
        AdditionalAnswers.delegatesTo(FederationClientMockFactory.create(this.entities)));
  }

  @Test
  void disallowedEcLocationLeavesSubordinateOut() throws Exception {
    this.withLeafEcLocation("http://hosting.example.com/leaf/ec");

    this.load();

    assertNull(this.cache.getData(LEAF_ID, 1L));
    assertEquals(List.of(LEAF_ID + ":FETCH_SUBORDINATE_STATEMENT"), this.errors);
    verify(this.client, never()).entityConfiguration(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().entityID().getValue())));
  }

  @Test
  void allowedEcLocationIsUsedForFetching() throws Exception {
    this.withLeafEcLocation("https://hosting.example.com/leaf/ec");

    this.load();

    final ScrapedEntity leaf = this.cache.getData(LEAF_ID, 1L);
    assertNotNull(leaf);
    assertEquals("https://hosting.example.com/leaf/ec", leaf.getEcLocation());
    assertEquals(List.of(), this.errors);
  }

  private void withLeafEcLocation(final String ecLocation) throws Exception {
    final SignedJWT original = this.entities.imLeafSub;
    final SignedJWT statement = new SignedJWT(new JWSHeader.Builder(original.getHeader()).build(),
        new JWTClaimsSet.Builder(original.getJWTClaimsSet()).claim("ec_location", ecLocation).build());
    statement.sign(new RSASSASigner(this.entities.imKey.toRSAKey()));
    doReturn(statement).when(this.client).fetch(
        argThat(r -> r != null && LEAF_ID.equals(r.parameters().subject())));
  }

  private void load() {
    new EntityStatementTreeLoader(this.client, new DFSExecution(), (key, stepName) -> {
      this.errors.add(key.getKey() + ":" + stepName.name());
      return new AtomicIntegerErrorContext();
    }, new EcLocationValidator(false)).resolveTree(TA_ID, new Tree<>(this.cache), 1L);
  }
}
