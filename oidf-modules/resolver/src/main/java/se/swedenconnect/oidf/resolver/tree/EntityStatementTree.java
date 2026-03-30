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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.SearchRequest;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class that wraps an internal federation tree. Defines what search queries should be executed towards the
 * tree.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityStatementTree {
  private final Tree<ScrapedEntity> tree;

  /**
   * @param tree    with federation nodes
   */
  public EntityStatementTree(final Tree<ScrapedEntity> tree) {
    this.tree = tree;
  }

  /**
   * Searches for a given entity and resolves the trustchain for that entity.
   *
   * @param resolveRequest with search parameters
   * @return resolved trust chain
   */
  public ResolverTrustChain getTrustChain(final ResolveRequest resolveRequest) {
    // Commit to current version
    final CacheSnapshot<ScrapedEntity> snapshot = this.tree.getCurrentSnapshot();
    // Find the entity that matches our subject, include parents
    final SearchRequest<ScrapedEntity> request = new SearchRequest<>(resolveRequest.asPredicate(), true, snapshot);
    try {
      final SequencedSet<ScrapedEntity> reversed = this.tree.search(request).stream()
          //Sort by level in tree
          .sorted(Comparator.comparingInt(a -> a.context().level()))
          .map(Tree.SearchResult::getData)
          .collect(Collectors.toCollection(LinkedHashSet::new))
          //Reverse order to be leaf --> n --> root
          .reversed();
      return this.resolverTrustChain(reversed);
    } catch (final IllegalStateException e) {
      log.error("Failed to load from cache due to internal error for request {}", resolveRequest);
      throw e;
    }
  }

  private ResolverTrustChain resolverTrustChain(final SequencedSet<ScrapedEntity> entities) {
    //1. Use request to find path to the entity
    //2. Initial chain structure should be
    // leaf --> subordinateStatement --> (node --> subordinateStatement [repeated]) --> root
    //3. Remove all nodes in the chain that is not leaf or root
    final List<ScrapedEntity> entityList = new ArrayList<>(entities);
    final LinkedHashSet<EntityStatement> chain = new LinkedHashSet<>();

    // Add leaf entity configuration
    chain.add(entityList.getFirst().getEntityStatement());

    // For each consecutive (child, parent) pair, add the parent's subordinate statement about
    // the child. Intermediate self-configurations are omitted per step 3.
    for (int i = 0; i < entityList.size() - 1; i++) {
      final ScrapedEntity child = entityList.get(i);
      final ScrapedEntity parent = entityList.get(i + 1);

      final ScrapedIntermediate parentIntermediate = parent.getIntermediate();
      if (parentIntermediate == null) {
        throw new IllegalStateException(
            "Entity %s has no intermediate role but appears as parent in trust chain"
                .formatted(parent.getEntityID().getValue()));
      }

      final SignedJWT subJWT = parentIntermediate.subordinates().get(child.getEntityID().getValue());
      if (subJWT == null) {
        throw new IllegalStateException(
            "Entity %s has no subordinate statement for %s"
                .formatted(parent.getEntityID().getValue(), child.getEntityID().getValue()));
      }

      try {
        chain.add(EntityStatement.parse(subJWT));
      } catch (final ParseException e) {
        throw new IllegalStateException("Failed to parse subordinate statement", e);
      }
    }

    // Add root (trust anchor) entity configuration
    if (entityList.size() > 1) {
      chain.add(entityList.getLast().getEntityStatement());
    }

    return new ResolverTrustChain(chain, entityList.getFirst());
  }

  /**
   * @param discoveryRequest to execute
   * @return list of resolved entities
   */
  public List<String> discovery(final DiscoveryRequest discoveryRequest) {
    return this.tree.search(new SearchRequest<>(discoveryRequest.asPredicate(), false, this.tree.getCurrentSnapshot()))
        .stream()
        .map(n -> n.getData().getEntityStatement().getEntityID().getValue())
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
   * @return the underlying tree
   */
  public Tree<ScrapedEntity> getTree() {
    return this.tree;
  }

  /**
   * @return snapshot of the current version of this tree
   */
  public CacheSnapshot<ScrapedEntity> getCurrentSnapshot() {
    return this.tree.getCurrentSnapshot();
  }

  /**
   * @param request defines search parameters
   * @return matching results
   */
  public Set<Tree.SearchResult<ScrapedEntity>> search(final SearchRequest<ScrapedEntity> request) {
    return this.tree.search(request);
  }

  /**
   * Get all entity statements in federation.
   * @return all ES
   */
  public Set<Tree.SearchResult<ScrapedEntity>> getAll() {
    return this.tree.search(new SearchRequest<>((parent, child) -> true, false, this.tree.getCurrentSnapshot()));
  }
}
