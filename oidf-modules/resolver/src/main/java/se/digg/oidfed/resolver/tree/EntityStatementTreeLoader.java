/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.resolver.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.tree.CacheSnapshot;
import se.digg.oidfed.common.tree.Node;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.resolver.tree.resolution.ErrorContext;
import se.digg.oidfed.resolver.tree.resolution.ErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;
import se.digg.oidfed.resolver.tree.resolution.StepExecutionError;
import se.digg.oidfed.resolver.tree.resolution.StepRecoveryStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Responsible for populating and creating a new (logical) tree.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityStatementTreeLoader {

  private final EntityStatementIntegration integration;

  private final ExecutionStrategy executionStrategy;

  private final StepRecoveryStrategy recoveryStrategy;

  private final ErrorContextFactory errorContextFactory;

  private final List<Runnable> postHooks = new ArrayList<>();

  /**
   * @param integration         to use for fetching statements
   * @param executionStrategy   to use when iterating through the federation
   * @param recoveryStrategy    to use when recovering from a failed step
   * @param errorContextFactory to use when creating new error contexts
   */
  public EntityStatementTreeLoader(
      final EntityStatementIntegration integration,
      final ExecutionStrategy executionStrategy,
      final StepRecoveryStrategy recoveryStrategy,
      final ErrorContextFactory errorContextFactory) {

    this.integration = integration;
    this.executionStrategy = executionStrategy;
    this.recoveryStrategy = recoveryStrategy;
    this.errorContextFactory = errorContextFactory;
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
   * @param rootLocation location of the root (trust-anchor)
   * @param tree         to add the nodes to
   */
  public void resolveTree(final String rootLocation, final Tree<EntityStatement> tree) {
    this.resolveTree(rootLocation, tree, null);
  }


  private void resolveTree(final String rootLocation, final Tree<EntityStatement> tree, final ErrorContext context) {
    try {
      final Node<EntityStatement> root = new Node<>(rootLocation);
      final EntityStatement entityStatement = this.integration.getEntityStatement(rootLocation);
      final CacheSnapshot<EntityStatement> snapshot = tree.addRoot(root, entityStatement);
      final String key = root.getKey();
      this.executionStrategy.execute(() -> this.resolveChildren(snapshot.getData(key), rootLocation, tree,
          snapshot, null));
      this.postHooks.forEach(this.executionStrategy::finalize);
    } catch (final Exception e) {
      log.error("TreeLoader [parent] resolution step failed: ", e);
      final ErrorContext errorContext = Optional.ofNullable(context)
          .orElseGet(() -> this.errorContextFactory.create(rootLocation))
          .increment();
      final StepExecutionError executionError = new StepExecutionError(
          rootLocation,
          c -> this.resolveTree(rootLocation, tree, c),
          errorContext
      );
      this.recoveryStrategy.handle(executionError);
    }
  }

  private void resolveChildren(
      final EntityStatement parent,
      final String parentLocation,
      final Tree<EntityStatement> tree,
      final CacheSnapshot<EntityStatement> snapshot,
      final ErrorContext context
  ) {
    try {
      final Optional<LocationInformationFactory.AuthorityInformation> authorityInformation = LocationInformationFactory
          .getAuthorityInformation(parent);

      authorityInformation.ifPresentOrElse(
          authority -> this.onAuthority(parentLocation, tree, authority, snapshot, context),
          () -> this.onSubjectStatement(parent, parentLocation, tree, snapshot, context)
      );
    } catch (final Exception e) {
      log.error("TreeLoader [child] resolution step failed: ", e);
      final ErrorContext errorContext = Optional.ofNullable(context)
          .orElseGet(() -> this.errorContextFactory.create(parentLocation))
          .increment();
      this.recoveryStrategy.handle(new StepExecutionError(parentLocation, this.childResolutionStep(parent,
          parentLocation, tree,
          snapshot), errorContext));
    }
  }

  private Consumer<ErrorContext> childResolutionStep(final EntityStatement parent,
                                                     final String parentLocation,
                                                     final Tree<EntityStatement> tree,
                                                     final CacheSnapshot<EntityStatement> snapshot) {
    return context -> this.resolveChildren(parent, parentLocation, tree, snapshot, context);
  }

  private void onAuthority(
      final String parentLocation,
      final Tree<EntityStatement> tree,
      final LocationInformationFactory.AuthorityInformation authorityInformation,
      final CacheSnapshot<EntityStatement> snapshot,
      final ErrorContext context
  ) {

    final List<String> subordinateListing = this.integration.getSubordinateListing(authorityInformation.listEndpoint());

    subordinateListing
        .forEach(subordinateIdentity -> {
          final String location = authorityInformation.subjectFetchEndpoint(subordinateIdentity);
          final EntityStatement entityStatement = this.integration.getEntityStatement(location);
          final Node<EntityStatement> node = new Node<>(location);
          tree.addChild(node, parentLocation, entityStatement, snapshot);
          this.executionStrategy.execute(() -> this.resolveChildren(entityStatement, location, tree, snapshot,
              context));
        });
  }

  private void onSubjectStatement(final EntityStatement parent, final String parentLocation,
                                  final Tree<EntityStatement> tree, final CacheSnapshot<EntityStatement> snapshot,
                                  final ErrorContext context) {
    LocationInformationFactory.getSubjectInformation(parent).ifPresent(subjectInformation -> {
      //If this is a subject statement, fetch entity statement
      subjectInformation.configurationLocation().ifPresentOrElse(embeddedLocation -> {
        //Entity configuration location is set, statement is located elsewhere or embedded in the claim
        String key = embeddedLocation;
        if (embeddedLocation.contains("data:application/entity-statement+jwt")) {
          key = "%s#ec".formatted(parentLocation);
        }
        final EntityStatement embeddedEntityStatement = this.integration.getEntityStatement(embeddedLocation);
        final Node<EntityStatement> embeddedNode = new Node<>(key);
        tree.addChild(embeddedNode, parentLocation, embeddedEntityStatement, snapshot);
      }, () -> {
        //Look for entity configuration at well-known location
        final EntityStatement entityStatement = this.integration.getEntityStatement(subjectInformation.location());
        final Node<EntityStatement> node = new Node<>(subjectInformation.location());
        tree.addChild(node, parentLocation, entityStatement, snapshot);
        this.executionStrategy.execute(() -> this.resolveChildren(entityStatement, subjectInformation.location(), tree,
            snapshot, context));
      });
    });
  }
}