package se.digg.oidfed.resolver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import se.digg.oidfed.common.jwt.SignerFactory;
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
  public static ResolverClient createTestResolver(final ResolverProperties properties, final FederationTree tree,
                                                  final StepRecoveryStrategy recoveryStrategy,
                                                  final SignerFactory adapter) {

    final VersionedInMemoryCache<EntityStatement> dataLayer = new VersionedInMemoryCache<>();
    final EntityStatementTree entityStatementTree = getEntityStatementTree(properties, tree,
        dataLayer, recoveryStrategy);

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
        () -> entityStatementTree.load(new EntityStatementTreeLoader(tree, new DFSExecution(), recoveryStrategy,
                new DefaultErrorContextFactory()).withAdditionalPostHook(
            dataLayer::useNextVersion),
        properties.trustAnchor() + "/.well-known/openid-federation"));
  }
  private static EntityStatementTree getEntityStatementTree(final ResolverProperties properties,
                                                            final FederationTree tree, final VersionedInMemoryCache<EntityStatement> entityStatementInMemoryDataLayer, final StepRecoveryStrategy recoveryStrategy) {
    final Tree<EntityStatement> internalTree = new Tree<>(entityStatementInMemoryDataLayer);
    final EntityStatementTree entityStatementTree = new EntityStatementTree(internalTree);

    final EntityStatementTreeLoader loader =
        new EntityStatementTreeLoader(tree, new DFSExecution(), recoveryStrategy, new DefaultErrorContextFactory()).withAdditionalPostHook(entityStatementInMemoryDataLayer::useNextVersion);

    entityStatementTree.load(loader, properties.trustAnchor() + "/.well-known/openid-federation");
    return entityStatementTree;
  }
}
