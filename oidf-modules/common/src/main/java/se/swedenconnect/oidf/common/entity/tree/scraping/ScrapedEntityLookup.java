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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.FederationTreeSource;
import se.swedenconnect.oidf.common.entity.tree.SearchRequest;
import se.swedenconnect.oidf.common.entity.tree.Tree;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Looks up scraped entities by entity ID across all trust anchor trees managed by the service.
 * Trees are fetched lazily from the {@link FederationTreeSource} on each request so newly
 * registered trees are always included. Each tree is queried against its own current snapshot
 * for deterministic state, and the most recently scraped match across all trees is returned.
 *
 * @author Felix Hellman
 */
public class ScrapedEntityLookup {

  private final FederationTreeSource treeSource;

  /**
   * Constructor.
   *
   * @param treeSource providing all managed trust anchor trees
   */
  public ScrapedEntityLookup(final FederationTreeSource treeSource) {
    this.treeSource = treeSource;
  }

  /**
   * Searches all managed trust anchor trees for an entity matching the given entity ID.
   * Each tree is queried against its own current snapshot (deterministic state per tree).
   * When the same entity appears in multiple trees, the most recently scraped instance is returned.
   *
   * @param entityId to search for
   * @return the most recently scraped matching entity, or empty if not found in any tree
   */
  public Optional<ScrapedEntity> findByEntityId(final String entityId) {
    return this.treeSource.getTrees().stream()
        .flatMap(tree -> {
          final CacheSnapshot<ScrapedEntity> snapshot = tree.getCurrentSnapshot();
          if (snapshot.getRoot() == null) {
            return Stream.empty();
          }
          final SearchRequest<ScrapedEntity> request = new SearchRequest<>(
              (entity, context) -> entity.getEntityID().getValue().equals(entityId),
              false,
              snapshot
          );
          return tree.search(request).stream().map(Tree.SearchResult::getData);
        })
        .max(Comparator.comparing(ScrapedEntity::getScrapedAt));
  }

  /**
   * Finds an entity by entity ID that acts as a trust anchor (has intermediate data).
   *
   * @param entityId to search for
   * @return matching entity with intermediate data, or empty if not found
   */
  public Optional<ScrapedEntity> findTrustAnchorByEntityId(final String entityId) {
    return this.findByEntityId(entityId)
        .filter(entity -> Objects.nonNull(entity.getIntermediate()));
  }

  /**
   * Finds an entity by entity ID that acts as a trust mark issuer.
   *
   * @param entityId to search for
   * @return matching entity with trust mark issuer data, or empty if not found
   */
  public Optional<ScrapedEntity> findTrustMarkIssuer(final String entityId) {
    return this.findByEntityId(entityId)
        .filter(entity -> Objects.nonNull(entity.getTrustMarkIssuer()));
  }
}
