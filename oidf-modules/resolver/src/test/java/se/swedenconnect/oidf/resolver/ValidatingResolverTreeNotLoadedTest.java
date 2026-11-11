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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.exception.InvalidTrustChainException;
import se.swedenconnect.oidf.common.entity.exception.TemporarilyUnavailableException;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.VersionedInMemoryCache;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;

import java.time.Duration;

/**
 * Tests the responses of a resolver whose tree has not been loaded, as before the first load has succeeded.
 */
class ValidatingResolverTreeNotLoadedTest {

  private static final String TA = "https://ta.example.com";
  private static final String LEAF = "https://leaf.example.com";

  private ValidatingResolver resolver;

  @BeforeEach
  void setUp() {
    this.resolver = this.resolverFor(new EntityStatementTree(new Tree<>(new VersionedInMemoryCache())));
  }

  @Test
  void resolveIsTemporarilyUnavailable() {
    final TemporarilyUnavailableException exception = Assertions.assertThrows(
        TemporarilyUnavailableException.class,
        () -> this.resolver.resolve(new ResolveRequest(LEAF, TA, null, false)));

    Assertions.assertEquals(503, exception.httpStatusCode());
    Assertions.assertEquals("temporarily_unavailable", exception.toJSONObject().get("error"));
  }

  @Test
  void resolveOfTheTrustAnchorIsTemporarilyUnavailable() {
    Assertions.assertThrows(TemporarilyUnavailableException.class,
        () -> this.resolver.resolve(new ResolveRequest(TA, TA, null, false)));
  }

  @Test
  void discoveryIsTemporarilyUnavailable() {
    final TemporarilyUnavailableException exception = Assertions.assertThrows(
        TemporarilyUnavailableException.class,
        () -> this.resolver.discovery(new DiscoveryRequest(TA, null, null)));

    Assertions.assertEquals(503, exception.httpStatusCode());
  }

  @Test
  void aBrokenTrustChainIsStillAnInvalidTrustChain() {
    final EntityStatementTree tree = Mockito.mock(EntityStatementTree.class);
    Mockito.when(tree.getTrustChainViaAuthorityHints(Mockito.any()))
        .thenThrow(new IllegalStateException("Entity %s has no subordinate statement for %s".formatted(TA, LEAF)));

    Assertions.assertThrows(InvalidTrustChainException.class,
        () -> this.resolverFor(tree).resolve(new ResolveRequest(LEAF, TA, null, false)));
  }

  private ValidatingResolver resolverFor(final EntityStatementTree tree) {
    return new ValidatingResolver(new ResolverProperties(TA, Duration.ofHours(1), null, "https://resolver"),
        Mockito.mock(ChainValidator.class), tree, new MetadataProcessor(),
        Mockito.mock(ResolverResponseFactory.class));
  }
}
