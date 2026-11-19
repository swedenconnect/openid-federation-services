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

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.resolver.chain.ChainValidationError;
import se.swedenconnect.oidf.resolver.chain.ChainValidationErrorType;
import se.swedenconnect.oidf.resolver.chain.ChainValidationFailedException;
import se.swedenconnect.oidf.resolver.chain.ChainValidationResult;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests {@link ValidatingResolver#explain(ResolveRequest)}.
 *
 * @author Felix Hellman
 */
class ValidatingResolverExplainTest {

  private static final String TA = "https://ta.example.com";
  private static final String LEAF = "https://leaf.example.com";

  @Test
  void causeAlreadyInTheMessageIsNotRepeated() throws Exception {
    final TestEntitiesFactory entities = new TestEntitiesFactory();
    final List<SignedJWT> chain = List.of(entities.leafEC, entities.imLeafSub, entities.taEC);
    final EntityStatementTree tree = Mockito.mock(EntityStatementTree.class);
    Mockito.when(tree.getTrustChainViaAuthorityHints(Mockito.any()))
        .thenReturn(Optional.of(new ResolverTrustChain(new LinkedHashSet<>(chain), null)));
    final ChainValidator validator = Mockito.mock(ChainValidator.class);
    final ChainValidationError error = new ChainValidationError(ChainValidationErrorType.LEAF_SIGNATURE_INVALID,
        "Invalid leaf statement: Entity statement has no kid header",
        new IllegalArgumentException("Entity statement has no kid header"));
    Mockito.when(validator.validate(Mockito.any())).thenReturn(new ChainValidationResult(chain,
        List.of(new ChainValidationFailedException("summary")), List.of(error)));
    final ValidatingResolver resolver = new ValidatingResolver(
        new ResolverProperties(TA, Duration.ofHours(1), null, "https://resolver"),
        validator, tree, new MetadataProcessor(), Mockito.mock(ResolverResponseFactory.class));

    final Map<Integer, Map<String, String>> explanation =
        resolver.explain(new ResolveRequest(LEAF, TA, null, true));

    final List<String> messages = explanation.values().stream()
        .filter(entry -> "LEAF_SIGNATURE_INVALID".equals(entry.get("type")))
        .map(entry -> entry.get("message"))
        .toList();
    Assertions.assertEquals(List.of("Invalid leaf statement: Entity statement has no kid header"), messages);
    Assertions.assertTrue(explanation.values().stream().noneMatch(entry -> entry.containsValue("summary")),
        explanation.toString());
  }
}
