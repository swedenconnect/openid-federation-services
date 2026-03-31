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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.ExecutionStrategy;
import se.swedenconnect.oidf.resolver.tree.resolution.ResolutionContext;
import se.swedenconnect.oidf.resolver.tree.resolution.StepExecutionError;
import se.swedenconnect.oidf.resolver.tree.resolution.StepRecoveryStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  public void resolveTree(final String trustAnchorEntityId, final Tree<ScrapedEntity> tree) {
    this.resolveTree(
        new NodeKey(trustAnchorEntityId, trustAnchorEntityId),
        tree,
        this.errorContextFactory.createEmpty(),
        new ResolutionContext());
  }


  void resolveTree(
      final NodeKey nodeKey,
      final Tree<ScrapedEntity> tree,
      final ErrorContext context,
      final ResolutionContext resolutionContext) {

    final Node<ScrapedEntity> root = new Node<>(nodeKey);
    final ScrapedEntity scrapedEntity =
        ScrapedEntity.builder().trustMarkStatuses(new HashMap<>())
            .entityID(new EntityID(nodeKey.issuer())).build();
    scrapedEntity.scrape(this.client, null);
    final EntityStatementWrapper wrapper =
        new EntityStatementWrapper(scrapedEntity.getEntityStatement().getSignedStatement());
    resolutionContext.setTrustAnchorEntityStatement(wrapper);
    final CacheSnapshot<ScrapedEntity> snapshot = tree.addRoot(root, scrapedEntity);
    final NodeKey key = root.getKey();
    this.executionStrategy.execute(() -> {
      if (scrapedEntity.getIntermediate() != null) {
        scrapedEntity.getIntermediate().subordinates().forEach((name, jwt) -> {
          this.resolveSubordinate(jwt, key, tree, snapshot, context, resolutionContext);
        });
      }
    });
    this.postHooks.forEach(this.executionStrategy::finalize);
  }

    void resolveSubordinate ( final SignedJWT subordinateStatement,
    final NodeKey parentKey,
    final Tree<ScrapedEntity> tree,
    final CacheSnapshot<ScrapedEntity> snapshot,
    final ErrorContext context,
    final ResolutionContext resolutionContext){
      try {
        final String subject = subordinateStatement.getJWTClaimsSet().getSubject();
        if (!resolutionContext.add(subject)) {
          return;
        }
        final Node<ScrapedEntity> subNode = new Node<>(NodeKey.fromSignedJwt(subordinateStatement));
        final ScrapedEntity entity =
            ScrapedEntity.builder().trustMarkStatuses(new HashMap<>()).entityID(new EntityID(subject)).build();
        entity.scrape(this.client, resolutionContext.getTrustAnchorEntityStatement());
        tree.addChild(subNode, parentKey, entity, snapshot);
        if (entity.getIntermediate() != null) {
          entity.getIntermediate().subordinates().forEach((name, jwt) -> {
            this.resolveSubordinate(jwt, subNode.getKey(), tree, snapshot, context, resolutionContext);
          });
        }
      } catch (final Exception e) {
        this.handleError(StepName.FETCH_SUBORDINATE_STATEMENT, parentKey,
            (c) -> this.resolveSubordinate(
                subordinateStatement, parentKey, tree, snapshot, c, resolutionContext
            ),
            context, e
        );
      }
    }

    void handleError (
    final StepName stepName,
    final NodeKey node,
    final Consumer<ErrorContext> step,
    final ErrorContext context,
    final Exception e
  ){
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

