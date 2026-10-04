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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for {@link EntityStatementTree#getTrustChainViaAuthorityHints(ResolveRequest)}.
 *
 * @author Martin Lindström
 */
class EntityStatementTreeAuthorityHintsTest {

  private static final String TA = "https://ta.example.com";
  private static final String LEAF = "https://leaf.example.com";
  private static final String A = "https://a.example.com";
  private static final String B = "https://b.example.com";

  private final Map<String, ScrapedEntity> nodes = new HashMap<>();
  private EntityStatementTree tree;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    final Tree<ScrapedEntity> mock = Mockito.mock(Tree.class);
    Mockito.when(mock.getNode(Mockito.any())).thenAnswer(
        invocation -> this.nodes.get(invocation.getArgument(0, NodeKey.class).getKey()));
    this.tree = new EntityStatementTree(mock);
  }

  @Test
  void loopInAuthorityHintsIsSkipped() {
    this.entity(LEAF, List.of(A), List.of());
    this.entity(A, List.of(B), List.of(LEAF, B));
    this.entity(B, List.of(A), List.of(A));
    this.entity(TA, null, List.of());

    Assertions.assertTrue(this.resolve().isEmpty());
  }

  @Test
  void hintWithoutSubordinateStatementIsSkipped() {
    this.entity(LEAF, List.of(A, B), List.of());
    this.entity(A, List.of(TA), List.of());
    this.entity(B, List.of(TA), List.of(LEAF));
    this.entity(TA, null, List.of(A, B));

    Assertions.assertEquals(List.of(LEAF, B, TA), this.issuers(this.resolve().orElseThrow()));
  }

  @Test
  void deadEndIsLeftForNextHint() {
    this.entity(LEAF, List.of(A, B), List.of());
    this.entity(A, List.of("https://unknown.example.com"), List.of(LEAF));
    this.entity(B, List.of(TA), List.of(LEAF));
    this.entity(TA, null, List.of(B));

    Assertions.assertEquals(List.of(LEAF, B, TA), this.issuers(this.resolve().orElseThrow()));
  }

  private Optional<ResolverTrustChain> resolve() {
    return this.tree.getTrustChainViaAuthorityHints(new ResolveRequest(LEAF, TA, null, false));
  }

  /**
   * Gets the issuers of the statements in the chain. The subordinate statements are issued by the superiors, so the
   * issuers of leaf configuration, statements and trust anchor configuration name the path.
   */
  private List<String> issuers(final ResolverTrustChain chain) {
    return chain.getTrustChain().stream().map(jwt -> {
      try {
        return jwt.getJWTClaimsSet().getIssuer();
      }
      catch (final java.text.ParseException e) {
        throw new IllegalStateException(e);
      }
    }).distinct().toList();
  }

  private void entity(final String id, final List<String> authorityHints, final List<String> subordinates) {
    final Map<String, SignedJWT> statements = new HashMap<>();
    subordinates.forEach(sub -> statements.put(sub, statement(id, sub, null)));
    this.nodes.put(id, ScrapedEntity.builder()
        .entityID(new EntityID(id))
        .entityStatement(statement(id, id, authorityHints))
        .intermediate(subordinates.isEmpty() ? null : new ScrapedIntermediate(statements))
        .build());
  }

  private static SignedJWT statement(final String issuer, final String subject, final List<String> authorityHints) {
    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer).subject(subject);
    if (authorityHints != null) {
      claims.claim("authority_hints", authorityHints);
    }
    return new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
  }
}
