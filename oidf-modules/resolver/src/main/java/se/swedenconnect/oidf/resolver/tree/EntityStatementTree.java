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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.SearchRequest;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class that wraps an internal federation tree. Defines what search queries should be executed towards the
 * tree.
 *
 * @author Felix Hellman
 */
public class EntityStatementTree {
  private final Tree<EntityStatement> tree;

  /**
   * @param tree with federation nodes
   */
  public EntityStatementTree(final Tree<EntityStatement> tree) {
    this.tree = tree;
  }

  /**
   * Searches for a given entity and resolves the trustchain for that entity.
   *
   * @param resolveRequest with search parameters
   * @return resolved trust chain
   */
  public Set<EntityStatement> getTrustChain(final ResolveRequest resolveRequest) {
    // Commit to current version
    final CacheSnapshot<EntityStatement> snapshot = this.tree.getCurrentSnapshot();
    // Find the entity that matches our subject, include parents
    final SearchRequest<EntityStatement> request = new SearchRequest<>(resolveRequest.asPredicate(), true, snapshot);

    return this.tree.search(request).stream()
        //Sort by level in tree
        .sorted(Comparator.comparingInt(a -> a.context().level()))
        .map(Tree.SearchResult::getData)
        // Remove intermediate authorities
        .filter(es -> !this.isIntermediate(es, resolveRequest))
        .collect(Collectors.toCollection(LinkedHashSet::new))
        //Reverse order to be leaf --> n --> root
        .reversed();
  }

  /**
   * @param discoveryRequest to execute
   * @return list of resolved entities
   */
  public List<String> discovery(final DiscoveryRequest discoveryRequest) {
    return this.tree.search(new SearchRequest<>(discoveryRequest.asPredicate(), false, this.tree.getCurrentSnapshot()))
        .stream()
        .map(n -> n.getData().getEntityID().getValue())
        .toList();
  }

  /**
   * Takes an {@link EntityStatementTreeLoader} and loads the tree from a root location
   *
   * @param loader              to use
   * @param trustAnchorEntityId to start resolution from
   */
  public void load(final EntityStatementTreeLoader loader, final String trustAnchorEntityId) {
    loader.resolveTree(trustAnchorEntityId, this.tree);
  }

  private boolean isIntermediate(final EntityStatement statement, final ResolveRequest request) {
    if (statement.getEntityID().getValue().equals(request.subject())) {
      //The target is an intermediate, but is also the intended search target
      return false;
    }
    if (!statement.getClaimsSet().isSelfStatement()) {
      return false;
    }
    if (statement.getEntityID().getValue().equalsIgnoreCase(request.trustAnchor())) {
      //This statment is the trust anchor and is not an intermediate
      return false;
    }
    final JSONObject metadata = statement.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY);
    //Intermediates and trust anchors MUST have a federation_fetch_endpoint
    return Objects.nonNull(metadata) && metadata.containsKey("federation_fetch_endpoint");
  }

  /**
   * Get all entity statements in federation.
   * @return all ES
   */
  public Set<Tree.SearchResult<EntityStatement>> getAll() {
    return this.tree.search(new SearchRequest<>((parent, child) -> true, false, this.tree.getCurrentSnapshot()));
  }
}
