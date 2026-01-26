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
package se.swedenconnect.oidf.resolver.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.ExecutionStrategy;
import se.swedenconnect.oidf.resolver.tree.resolution.ResolutionContext;
import se.swedenconnect.oidf.resolver.tree.resolution.StepExecutionError;
import se.swedenconnect.oidf.resolver.tree.resolution.StepRecoveryStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Responsible for populating and creating a new (logical) tree.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityStatementTreeLoader {

  /**
   * Name of entity statement tree loader steps
   */
  public enum StepName {
    /**
     * To be used only for empty error context
     */
    NONE,
    /**
     * When resolving the root of the tree
     */
    RESOLVE_ROOT,
    /**
     * When resolving a subordinate listing
     */
    SUBORDINATE_LISTING,
    /**
     * When resolving a subordinate statement
     */
    FETCH_SUBORDINATE_STATEMENT,
    /**
     * When fetching entity configuration (that is not root)
     */
    FETCH_ENTITY_CONFIGURATION
  }

  /**
   * Number of tries to try a step before using cached values.
   */
  private final int useCacheThreshold;

  private final FederationClient client;

  private final ExecutionStrategy executionStrategy;

  private final StepRecoveryStrategy recoveryStrategy;

  private final ErrorContextFactory errorContextFactory;

  private final List<Runnable> postHooks = new ArrayList<>();

  /**
   * @param client              to use for fetching statements
   * @param executionStrategy   to use when iterating through the federation
   * @param recoveryStrategy    to use when recovering from a failed step
   * @param errorContextFactory to use when creating new error contexts
   * @param useCacheThreshold   how many times a step can fail before a cached value is considered
   */
  public EntityStatementTreeLoader(
      final FederationClient client,
      final ExecutionStrategy executionStrategy,
      final StepRecoveryStrategy recoveryStrategy,
      final ErrorContextFactory errorContextFactory,
      final int useCacheThreshold) {

    this.client = client;
    this.executionStrategy = executionStrategy;
    this.recoveryStrategy = recoveryStrategy;
    this.errorContextFactory = errorContextFactory;
    this.useCacheThreshold = useCacheThreshold;
  }

  /**
   * Adds a post-hook to run when the statement tree has loaded a new federation tree.
   *
   * @param hook to add
   * @return this
   */
  public EntityStatementTreeLoader withAdditionalPostHook(final Runnable hook) {
    this.postHooks.add(hook);
    return this;
  }

  /**
   * Resolves the tree from a given location (trust-anchor)
   *
   * @param trustAnchorEntityId location of the root (trust-anchor)
   * @param tree                to add the nodes to
   */
  public void resolveTree(final String trustAnchorEntityId, final Tree<EntityStatement> tree) {
    this.resolveTree(
        new NodeKey(trustAnchorEntityId, trustAnchorEntityId),
        tree,
        this.errorContextFactory.createEmpty(),
        new ResolutionContext());
  }


  void resolveTree(
      final NodeKey nodeKey,
      final Tree<EntityStatement> tree,
      final ErrorContext context,
      final ResolutionContext resolutionContext) {
    try {
      final Node<EntityStatement> root = new Node<>(nodeKey);
      final EntityConfigurationRequest entityConfigurationRequest =
          new EntityConfigurationRequest(new EntityID(nodeKey.issuer()), null);
      final EntityStatement entityStatement =
          this.client.entityConfiguration(new FederationRequest<>(
              entityConfigurationRequest,
              Map.of(),
              this.useCachedValue(context)));
      final CacheSnapshot<EntityStatement> snapshot = tree.addRoot(root, entityStatement);
      final NodeKey key = root.getKey();
      this.executionStrategy.execute(() -> this.subordinateListing(snapshot.getData(key), nodeKey, tree,
          snapshot, this.errorContextFactory.createEmpty(), resolutionContext));
      this.postHooks.forEach(this.executionStrategy::finalize);
    } catch (final Exception e) {
      this.handleError(StepName.RESOLVE_ROOT,
          nodeKey,
          c -> this.resolveTree(nodeKey, tree, c, resolutionContext),
          context, e
      );
    }
  }

  void subordinateListing(
      final EntityStatement parent,
      final NodeKey parentKey,
      final Tree<EntityStatement> tree,
      final CacheSnapshot<EntityStatement> snapshot,
      final ErrorContext context, final ResolutionContext resolutionContext
  ) {
    if (!((Map<String, Object>) parent.getClaimsSet().getClaim("metadata"))
        .containsKey(EntityType.FEDERATION_ENTITY.getValue())) {
      //Stop resolving if entity does not declare federation entity metadata.
      return;
    }
    log.debug("Resolving %s".formatted(parentKey.getKey()));
    try {
      final JSONObject metadata = parent.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY);
      final FederationEntityMetadata parse = FederationEntityMetadata.parse(metadata);
      if (Objects.nonNull(parse.getFederationListEndpointURI())
          && Objects.nonNull(parse.getFederationFetchEndpointURI())) {
        // Entity is intermediate
        final SubordinateListingRequest subordinateListingRequest = SubordinateListingRequest.requestAll();
        final FederationRequest<SubordinateListingRequest> request =
            new FederationRequest<>(
                subordinateListingRequest,
                metadata,
                this.useCachedValue(context));
        final List<String> subordinateListing = this.client.subordinateListing(request);
        subordinateListing.forEach(subordinate -> this.resolveSubordinate(subordinate, parentKey, tree, snapshot,
            this.errorContextFactory.createEmpty(),
            resolutionContext, metadata));
      }
    } catch (final Exception e) {
      this.handleError(
          StepName.SUBORDINATE_LISTING,
          parentKey,
          c -> this.subordinateListing(parent, parentKey, tree, snapshot, c, resolutionContext),
          context, e
      );
    }
  }

  void resolveSubordinate(final String subordinate,
                          final NodeKey parentKey,
                          final Tree<EntityStatement> tree,
                          final CacheSnapshot<EntityStatement> snapshot,
                          final ErrorContext context,
                          final ResolutionContext resolutionContext,
                          final Map<String, Object> metadataMap) {
    try {
      final EntityStatement subordinateStatement = this.client.fetch(
          new FederationRequest<>(
              new FetchRequest(subordinate),
              metadataMap,
              this.useCachedValue(context))
      );
      final Node<EntityStatement> subNode = new Node<>(NodeKey.fromEntityStatement(subordinateStatement));
      tree.addChild(subNode, parentKey, subordinateStatement, snapshot);
      this.resolveEntityConfiguration(tree, snapshot, this.errorContextFactory.createEmpty(), resolutionContext,
          subordinateStatement, subNode);
    } catch (final Exception e) {
      this.handleError(StepName.FETCH_SUBORDINATE_STATEMENT, parentKey,
          (c) -> this.resolveSubordinate(
              subordinate, parentKey, tree, snapshot, c, resolutionContext, metadataMap
          ),
          context, e
      );
    }
  }

  private void resolveEntityConfiguration(
      final Tree<EntityStatement> tree,
      final CacheSnapshot<EntityStatement> snapshot,
      final ErrorContext context,
      final ResolutionContext resolutionContext,
      final EntityStatement subordinateStatement,
      final Node<EntityStatement> subordinateNode) {
    final EntityID subjectEntityID = subordinateStatement.getClaimsSet().getSubjectEntityID();
    try {
      final JSONObject subordinateMetadata = Optional.ofNullable(
              subordinateStatement.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY))
          .orElse(new JSONObject());
      final EntityConfigurationRequest entityConfigurationRequest = new EntityConfigurationRequest(subjectEntityID,
          Optional.ofNullable(subordinateStatement
                  .getClaimsSet()
                  .getClaim("subject_entity_configuration_location"))
              .map(String.class::cast)
              .orElse(null));
      final EntityStatement entityConfiguration =
          this.client.entityConfiguration(new FederationRequest<>(entityConfigurationRequest,
              subordinateMetadata, this.useCachedValue(context)));
      final Node<EntityStatement> node = new Node<>(NodeKey.fromEntityStatement(entityConfiguration));
      tree.addChild(node, subordinateNode.getKey(), entityConfiguration, snapshot);
      this.executionStrategy.execute(() ->
          this.subordinateListing(
              entityConfiguration,
              node.getKey(),
              tree,
              snapshot,
              this.errorContextFactory.createEmpty(),
              resolutionContext)
      );
    } catch (final Exception e) {
      this.handleError(StepName.FETCH_ENTITY_CONFIGURATION, NodeKey.fromEntityStatement(subordinateStatement),
          (c) -> this.resolveEntityConfiguration(
              tree,
              snapshot,
              c,
              resolutionContext,
              subordinateStatement,
              subordinateNode
          ),
          context, e
      );
    }
  }

  private boolean useCachedValue(final ErrorContext context) {
    return context.getErrorCount() >= this.useCacheThreshold;
  }

  void handleError(
      final StepName stepName,
      final NodeKey node,
      final Consumer<ErrorContext> step,
      final ErrorContext context,
      final Exception e
  ) {
    log.error("TreeLoader {} {} failed with exception {} enable trace log for more details", node.getKey(),
        stepName.name(),
        e.getClass().getCanonicalName());
    log.trace("TreeLoader {} {} failed: ", node.getKey(), stepName.name(), e);
    final StepExecutionError error = new StepExecutionError(
        "%s_%s".formatted(stepName, node.getKey()),
        step,
        context.orElseGet(() -> this.errorContextFactory.create(node, stepName)));
    this.recoveryStrategy.handle(error);
  }
}

