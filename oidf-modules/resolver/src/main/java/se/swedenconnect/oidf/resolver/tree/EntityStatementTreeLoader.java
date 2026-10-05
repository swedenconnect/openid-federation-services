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
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.WrongJwkKidException;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.ExecutionStrategy;
import se.swedenconnect.oidf.resolver.tree.resolution.ResolutionContext;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

  private static final ExecutorService RESOLUTION_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  private final FederationClient client;

  private final ExecutionStrategy executionStrategy;

  private final ErrorContextFactory errorContextFactory;

  private final EcLocationValidator ecLocationValidator;

  private final List<Runnable> postHooks = new ArrayList<>();

  /**
   * @param client              to use for fetching statements
   * @param executionStrategy   to use when iterating through the federation
   * @param errorContextFactory to use when creating new error contexts
   * @param ecLocationValidator to check ec_location values of Subordinate Statements with
   */
  public EntityStatementTreeLoader(
      final FederationClient client,
      final ExecutionStrategy executionStrategy,
      final ErrorContextFactory errorContextFactory,
      final EcLocationValidator ecLocationValidator) {

    this.client = client;
    this.executionStrategy = executionStrategy;
    this.errorContextFactory = errorContextFactory;
    this.ecLocationValidator = ecLocationValidator;
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
   * @param snapshotId          shared snapshot version to use for this load
   */
  public void resolveTree(final String trustAnchorEntityId, final Tree<ScrapedEntity> tree, final long snapshotId) {
    this.resolveTree(
        new NodeKey(trustAnchorEntityId),
        tree,
        snapshotId,
        new ResolutionContext());
  }


  void resolveTree(
      final NodeKey nodeKey,
      final Tree<ScrapedEntity> tree,
      final long snapshotId,
      final ResolutionContext resolutionContext) {

    log.debug("TreeLoader resolving root {}", nodeKey.getKey());
    final Node<ScrapedEntity> root = new Node<>(nodeKey);
    final EntityID entityID = new EntityID(nodeKey.entityId());
    final ScrapedEntity scrapedEntity = ScrapedEntity.builder().entityID(entityID).build();
    scrapedEntity.scrape(this.client);
    log.debug("TreeLoader scraped root {}", nodeKey.getKey());
    final EntityStatementWrapper wrapper =
        new EntityStatementWrapper(scrapedEntity.getEntityStatement());
    resolutionContext.setTrustAnchorEntityStatement(wrapper);
    final CacheSnapshot<ScrapedEntity> snapshot = tree.addRoot(root, scrapedEntity, snapshotId);
    final NodeKey key = root.getKey();
    this.executionStrategy.execute(() -> {
      if (scrapedEntity.getIntermediate() != null) {
        final List<CompletableFuture<Void>> futures = scrapedEntity.getIntermediate().subordinates()
            .entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(
                () -> this.resolveSubordinate(entry.getValue(), key, tree, snapshot, resolutionContext),
                RESOLUTION_EXECUTOR))
            .toList();
        log.debug("TreeLoader root {} has {} subordinates to resolve", nodeKey.getKey(), futures.size());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      } else {
        log.debug("TreeLoader root {} has no subordinates", nodeKey.getKey());
      }
    });
    log.debug("TreeLoader finished resolving root {}, running post hooks", nodeKey.getKey());
    this.postHooks.forEach(this.executionStrategy::finalize);
  }

  /**
   * Scrapes a subordinate, adds it to the snapshot being built and continues with its own subordinates. If the scrape
   * fails, the data from the previous load is used while it is still valid.
   *
   * @param subordinateStatement the superior's statement about the subordinate
   * @param parentKey            key of the superior
   * @param tree                 being loaded
   * @param snapshot             being built
   * @param resolutionContext    of this load
   */
  void resolveSubordinate(final SignedJWT subordinateStatement,
                          final NodeKey parentKey,
                          final Tree<ScrapedEntity> tree,
                          final CacheSnapshot<ScrapedEntity> snapshot,
                          final ResolutionContext resolutionContext) {
    final String subject;
    final Object ecLocation;
    try {
      subject = subordinateStatement.getJWTClaimsSet().getSubject();
      ecLocation = subordinateStatement.getJWTClaimsSet().getClaim(EcLocationValidator.CLAIM_NAME);
    } catch (final Exception e) {
      this.handleError(StepName.FETCH_SUBORDINATE_STATEMENT, parentKey, e);
      return;
    }
    log.debug("TreeLoader resolving subordinate {} of {}", subject, parentKey.getKey());
    if (ecLocation != null && !this.isValidEcLocation(ecLocation, subject, parentKey)) {
      return;
    }
    if (!resolutionContext.add(subject)) {
      log.debug("TreeLoader skipping already visited subordinate {}", subject);
      return;
    }
    final Node<ScrapedEntity> subNode = new Node<>(new NodeKey(subject));
    ScrapedEntity entity;
    try {
      entity = ScrapedEntity.builder().entityID(new EntityID(subject)).ecLocation((String) ecLocation).build();
      entity.scrape(this.client);
      log.debug("TreeLoader scraped subordinate {}", subject);
    } catch (final Exception e) {
      entity = this.previousData(subNode.getKey(), tree, snapshot, e);
      if (entity == null) {
        this.handleError(StepName.FETCH_ENTITY_CONFIGURATION, subNode.getKey(), e);
        return;
      }
      this.errorContextFactory.create(subNode.getKey(), StepName.FETCH_ENTITY_CONFIGURATION).increment();
    }
    try {
      tree.addChild(subNode, parentKey, entity, snapshot);
      if (entity.getIntermediate() != null) {
        final List<CompletableFuture<Void>> futures = entity.getIntermediate().subordinates()
            .entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(
                () -> this.resolveSubordinate(entry.getValue(), subNode.getKey(), tree, snapshot,
                    resolutionContext),
                RESOLUTION_EXECUTOR))
            .toList();
        log.debug("TreeLoader subordinate {} has {} subordinates to resolve", subject, futures.size());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      } else {
        log.debug("TreeLoader subordinate {} has no further subordinates", subject);
      }
    } catch (final Exception e) {
      this.handleError(StepName.FETCH_SUBORDINATE_STATEMENT, parentKey, e);
    }
  }

  /**
   * Checks the ec_location claim of a Subordinate Statement. A subordinate whose statement has a disallowed value is
   * left out of the tree, and its Entity Configuration is not fetched.
   *
   * @param ecLocation the claim value
   * @param subject    the subject of the statement
   * @param parentKey  key of the issuer of the statement
   * @return true if the value is allowed
   */
  private boolean isValidEcLocation(final Object ecLocation, final String subject, final NodeKey parentKey) {
    try {
      if (!(ecLocation instanceof final String value)) {
        throw new IllegalArgumentException("ec_location is not a string");
      }
      this.ecLocationValidator.validate(value);
      return true;
    }
    catch (final IllegalArgumentException e) {
      log.info("TreeLoader leaving out {}, Subordinate Statement from {} is invalid: {}", subject,
          parentKey.getKey(), e.getMessage());
      this.errorContextFactory.create(new NodeKey(subject), StepName.FETCH_SUBORDINATE_STATEMENT).increment();
      return false;
    }
  }

  /**
   * Looks up the data of an entity in the snapshot that is in use while a new snapshot is built. The data is used
   * in place of a failed scrape, so that an entity that is temporarily unreachable is not dropped from the tree. Data
   * whose Entity Configuration has expired is not used, nor is data for an entity whose Entity Configuration is
   * invalid.
   *
   * @param key      of the entity
   * @param tree     being loaded
   * @param snapshot being built
   * @param e        cause of the failed scrape
   * @return a copy of the previous data, marked as coming from a failed scrape, or {@code null} if there is no data
   *     to use
   */
  ScrapedEntity previousData(
      final NodeKey key,
      final Tree<ScrapedEntity> tree,
      final CacheSnapshot<ScrapedEntity> snapshot,
      final Exception e) {

    if (e instanceof WrongJwkKidException) {
      return null;
    }
    final CacheSnapshot<ScrapedEntity> previous = tree.getCurrentSnapshot();
    if (previous.getVersion() == snapshot.getVersion()) {
      return null;
    }
    final ScrapedEntity data = previous.getData(key);
    if (data == null || data.getEntityStatement() == null) {
      return null;
    }
    final Instant now = Instant.now();
    final Date expiration;
    try {
      expiration = data.getEntityStatement().getJWTClaimsSet().getExpirationTime();
    } catch (final ParseException pe) {
      return null;
    }
    if (expiration == null || !expiration.toInstant().isAfter(now)) {
      log.debug("TreeLoader previous data of {} has expired", key.getKey());
      return null;
    }
    final Instant failedSince = Optional.ofNullable(data.getScrapeFailedAt()).orElse(now);
    log.warn("TreeLoader failed to fetch {} ({}), keeping data from previous load until {}",
        key.getKey(), e.getClass().getCanonicalName(), expiration.toInstant());
    log.trace("TreeLoader failed to fetch {}: ", key.getKey(), e);
    return data.copyForFailedScrape(failedSince);
  }

  /**
   * Records a failed step. The failing branch is left out of the snapshot being built; it is picked up
   * again by the next scheduled tree load rather than retried against the current one. A failed scrape of an
   * entity that has valid data in the previous load does not end up here, see
   * {@link #previousData(NodeKey, Tree, CacheSnapshot, Exception)}.
   *
   * @param stepName of the step that failed
   * @param node     the step was executing for
   * @param e        cause of the failure
   */
  void handleError(
      final StepName stepName,
      final NodeKey node,
      final Exception e
  ) {
    log.error("TreeLoader {} {} failed with exception {} enable trace log for more details", node.getKey(),
        stepName.name(),
        e.getClass().getCanonicalName());
    log.trace("TreeLoader {} {} failed: ", node.getKey(), stepName.name(), e);
    this.errorContextFactory.create(node, stepName).increment();
  }
}
