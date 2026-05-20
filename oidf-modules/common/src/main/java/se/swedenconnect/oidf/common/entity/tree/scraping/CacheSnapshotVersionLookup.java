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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import se.swedenconnect.oidf.common.entity.tree.FederationTreeSource;

/**
 * Looks up scraped entities by entity ID across all trust anchor trees managed by the service.
 * Trees are fetched lazily from the {@link FederationTreeSource} on each request so newly
 * registered trees are always included. Each tree is queried against its own current snapshot
 * for deterministic state, and the most recently scraped match across all trees is returned.
 *
 * @author Felix Hellman
 */
public class CacheSnapshotVersionLookup {

  private final FederationTreeSource treeSource;

  /**
   * Constructor.
   *
   * @param treeSource providing all managed trust anchor trees
   */
  public CacheSnapshotVersionLookup(final FederationTreeSource treeSource) {
    this.treeSource = treeSource;
  }

  /**
   * Returns the current snapshot version. Since all trees share the same snapshot ID, the version
   * is read from the first available tree.
   *
   * @return the current snapshot version, or 0 if no trees are present
   */
  public Long getLatestSnapshotVersion() {
    return this.treeSource.getTrees().stream()
        .mapToLong(tree -> tree.getCurrentSnapshot().getVersion())
        .findFirst()
        .orElse(0L);
  }
}
