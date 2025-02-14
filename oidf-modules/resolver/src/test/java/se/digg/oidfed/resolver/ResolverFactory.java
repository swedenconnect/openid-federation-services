/*
 * Copyright 2024-2025 Sweden Connect
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
package se.digg.oidfed.resolver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.common.tree.VersionedInMemoryCache;
import se.digg.oidfed.resolver.chain.ChainValidationStep;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.chain.ConstraintsValidationStep;
import se.digg.oidfed.resolver.chain.CriticalClaimsValidationStep;
import se.digg.oidfed.resolver.chain.SignatureValidationStep;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.metadata.OIDFPolicyOperationFactory;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;
import se.digg.oidfed.resolver.tree.resolution.DFSExecution;
import se.digg.oidfed.resolver.tree.resolution.DefaultErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.StepRecoveryStrategy;

import java.time.Clock;
import java.util.List;

public class ResolverFactory {
  public static ResolverClient createTestResolver(final FederationClient client,
                                                  final ResolverProperties properties,
                                                  final StepRecoveryStrategy recoveryStrategy,
                                                  final SignerFactory adapter) {

    final ResolverCache dataLayer = new VersionedInMemoryCache();
    final EntityStatementTree entityStatementTree = getEntityStatementTree(client, properties, dataLayer, recoveryStrategy);

    final List<ChainValidationStep> chainValidationSteps = List.of(
        new ConstraintsValidationStep(),
        new CriticalClaimsValidationStep(),
        new SignatureValidationStep(new JWKSet(properties.trustedKeys()))
    );

    final ChainValidator validator = new ChainValidator(chainValidationSteps);
    final MetadataProcessor processor =
        new MetadataProcessor(new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator());
    final ResolverResponseFactory factory = new ResolverResponseFactory(Clock.systemUTC(), properties, adapter);

    final Resolver resolver = new Resolver(
        properties,
        validator,
        entityStatementTree,
        processor,
        factory);

    return new ResolverClient(resolver, properties.entityIdentifier(),
        adapter.getSignKey(),
        () -> entityStatementTree.load(new EntityStatementTreeLoader(client, new DFSExecution(), recoveryStrategy,
                new DefaultErrorContextFactory()).withAdditionalPostHook(
                dataLayer::useNextVersion),
            properties.trustAnchor() + "/.well-known/openid-federation"));
  }

  private static EntityStatementTree getEntityStatementTree(
      final FederationClient client,
      final ResolverProperties properties,
      final ResolverCache resolverCache,
      final StepRecoveryStrategy recoveryStrategy) {
    final Tree<EntityStatement> internalTree = new Tree<>(resolverCache);
    final EntityStatementTree entityStatementTree = new EntityStatementTree(internalTree);

    final EntityStatementTreeLoader loader =
        new EntityStatementTreeLoader(client, new DFSExecution(), recoveryStrategy,
            new DefaultErrorContextFactory()).withAdditionalPostHook(resolverCache::useNextVersion);

    entityStatementTree.load(loader, properties.trustAnchor() + "/.well-known/openid-federation");
    return entityStatementTree;
  }
}
