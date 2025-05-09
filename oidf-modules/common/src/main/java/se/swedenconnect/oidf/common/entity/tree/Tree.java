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
package se.swedenconnect.oidf.common.entity.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic tree structure to do graph-searches upon.
 * @param <T>
 *
 * @author Felix Hellman
 */
public class Tree<T> {
  private final SnapshotSource<T> snapshotSource;

  /**
   * Constructor.
   * @param snapshotSource to provide snapshots
   */
  public Tree(final SnapshotSource<T> snapshotSource) {
    this.snapshotSource = snapshotSource;
  }

  /**
   * @param node to add
   * @param key to find the parent of the child
   * @param data for the node
   * @param snapshot version to add the node to
   */
  public void addChild(final Node<T> node, final NodeKey key, final T data, final CacheSnapshot<T> snapshot) {
    snapshot.getRoot().addChild(new Node.NodePopulationContext<>(node, snapshot, key, new HashSet<>()));
    snapshot.setData(node.getKey(), data);
  }

  /**
   * Creates a new snapshot version with the given root.
   * @param node of the root
   * @param data of the root
   * @return snapshot of the next version
   */
  public CacheSnapshot<T> addRoot(final Node<T> node, final T data) {
    return this.snapshotSource.createNewSnapshot(node, data);
  }

  /**
   * Performs a search upon the tree.
   * @param request defines search parameters
   * @return a set of matching results
   */
  public Set<SearchResult<T>> search(final SearchRequest<T> request) {
    final Node<T> root = request.snapshot().getRoot();
    final HashSet<NodeKey> visisted = new HashSet<>(List.of(root.getKey()));
    final Node.NodeSearchContext<T> context =
        new Node.NodeSearchContext<>(0, request.includeParent(), request.snapshot(), visisted);
    return root.search(request.predicate(), context);
  }

  /**
   * @param request that specifies who to visit and what action to perform
   */
  public void visit(final VisitRequest<T> request) {
    final HashSet<NodeKey> visited = new HashSet<>();
    final Node.NodeSearchContext<T> context = new Node.NodeSearchContext<>(0, false, request.snapshot(), visited);
    request.snapshot().getRoot()
        .visit(request.searchPredicate(), request.visitor(), context);
  }

  /**
   * Individual result for a search.
   * @param node
   * @param context
   * @param <T>
   */
  public record SearchResult<T>(Node<T> node, Node.NodeSearchContext<T> context) {
    /**
     * @return data from search result
     */
    public T getData() {
      return this.context().cacheSnapshot().getData(this.node.getKey());
    }
  }

  /**
   * @return snapshot of current version
   */
  public CacheSnapshot<T> getCurrentSnapshot() {
    return this.snapshotSource.snapshot();
  }


}
