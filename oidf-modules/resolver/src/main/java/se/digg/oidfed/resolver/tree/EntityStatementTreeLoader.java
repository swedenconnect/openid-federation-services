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
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsible for populating and creating a new (logical) tree.
 *
 * @author Felix Hellman
 */
public class EntityStatementTreeLoader {

  private final EntityStatementIntegration integration;

  private final ExecutionStrategy executionStrategy;

  private final List<Runnable> postHooks = new ArrayList<>();

  /**
   * @param integration to use for fetching statements
   * @param executionStrategy to use when iterating through the federation
   */
  public EntityStatementTreeLoader(final EntityStatementIntegration integration,
      final ExecutionStrategy executionStrategy) {
    this.integration = integration;
    this.executionStrategy = executionStrategy;
  }

  /**
   * Adds a post-hook to run when the statement tree has loaded a new federation tree.
   *
   * @param hook to add
   * @return this
   */
  public EntityStatementTreeLoader withAdditionalPostHook(final Runnable hook) {
    postHooks.add(hook);
    return this;
  }

  /**
   * Resolves the tree from a given location (trust-anchor)
   * @param rootLocation location of the root (trust-anchor)
   * @param tree to add the nodes to
   */
  public void resolveTree(final String rootLocation, final Tree<EntityStatement> tree) {
    final Node<EntityStatement> root = new Node<>(rootLocation);
    final EntityStatement entityStatement = this.integration.getEntityStatement(rootLocation);
    final CacheSnapshot<EntityStatement> snapshot = tree.addRoot(root, entityStatement);
    final String key = root.getKey();
    executionStrategy.execute(() -> resolveChildren(snapshot.getData(key), rootLocation, tree, snapshot));
    postHooks.forEach(executionStrategy::finalize);
  }

  private void resolveChildren(final EntityStatement parent, final String parentLocation,
      final Tree<EntityStatement> tree, final CacheSnapshot<EntityStatement> snapshot) {
    final Optional<LocationInformationFactory.AuthorityInformation> authorityInformation = LocationInformationFactory
        .getAuthorityInformation(parent);

    authorityInformation.ifPresentOrElse(
        authority -> onAuthority(parentLocation, tree, authority, snapshot),
        () -> onSubjectStatement(parent, parentLocation, tree, snapshot)
    );
  }

  private void onAuthority(
      final String parentLocation,
      final Tree<EntityStatement> tree,
      final LocationInformationFactory.AuthorityInformation authorityInformation,
      final CacheSnapshot<EntityStatement> snapshot) {

    final List<String> subordinateListing = this.integration.getSubordinateListing(authorityInformation.listEndpoint());

    subordinateListing
        .forEach(subordinateIdentity -> {
          final String location = authorityInformation.subjectFetchEndpoint(subordinateIdentity);
          final EntityStatement entityStatement = this.integration.getEntityStatement(location);
          final Node<EntityStatement> node = new Node<>(location);
          tree.addChild(node, parentLocation, entityStatement, snapshot);
          executionStrategy.execute(() -> resolveChildren(entityStatement, location, tree, snapshot));
        });
  }

  private void onSubjectStatement(final EntityStatement parent, final String parentLocation,
      final Tree<EntityStatement> tree, final CacheSnapshot<EntityStatement> snapshot) {
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
        executionStrategy.execute(() -> resolveChildren(entityStatement, subjectInformation.location(), tree, snapshot));
      });
    });
  }
}