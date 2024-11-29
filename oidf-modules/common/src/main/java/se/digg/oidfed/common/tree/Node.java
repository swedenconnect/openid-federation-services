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
package se.digg.oidfed.common.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Logical node implementation that holds a key for data located elsewhere.
 * Implements generic search and visit operations.
 *
 * @param <T> data type
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class Node<T> {

  private String key;

  /**
   * Default constructor.
   */
  public Node() {
  }

  /**
   * Constructor.
   * @param key for this node
   */
  public Node(final String key) {
    this.key = key;
  }

  /**
   * Operation to add a child to a given node (or a node below this node)
   * @param context to propagate data in the tree
   */
  public void addChild(final NodePopulationContext<T> context) {
    if (context.parentKey().equalsIgnoreCase(this.key)) {
      context.cacheSnapshot().append(context.child(), this);
      return;
    }
    context.cacheSnapshot().getChildren(this).forEach(child -> child.addChild(context));
  }

  /**
   * @param searchPredicate what to search for
   * @param context to propagate data in the tree
   * @return a set of {@link Tree.SearchResult} for matching nodes given the search predicate
   */
  public Set<Tree.SearchResult<T>> search(final BiPredicate<T, NodeSearchContext<T>> searchPredicate,
      final NodeSearchContext<T> context) {
    final Set<Tree.SearchResult<T>> matches = new HashSet<>();
    if (searchPredicate.test(context.cacheSnapshot.getData(this.key), context)) {
      matches.add(new Tree.SearchResult<>(this, context));
    }
    final List<Node<T>> children = context.cacheSnapshot().getChildren(this);
    final Set<Tree.SearchResult<T>> results =
        children.stream()
            .flatMap(child -> child.search(searchPredicate, context.next()).stream())
            .collect(Collectors.toSet());

    matches.addAll(results);

    if (!results.isEmpty() && context.includeParent) {
      matches.add(new Tree.SearchResult<>(this, context));
    }
    return matches;
  }

  /**
   * @param searchPredicate what to search for
   * @param visitor action to perform upon a node
   * @param context to propagate data in the tree
   */
  public void visit(final BiPredicate<Node<T>, NodeSearchContext<T>> searchPredicate,
      final BiConsumer<Node<T>, Node<T>> visitor,
      final NodeSearchContext<T> context) {
    if (searchPredicate.test(this, context)) {
      context.cacheSnapshot.getChildren(this).forEach(child -> {
        visitor.accept(this, child);
        child.visit(searchPredicate, visitor, context.next());
      });
    }
  }

  /**
   * @param level of the tree
   * @param includeParent true if all parents of a search result should be included
   * @param cacheSnapshot snapshot of a specific version of the cache to search upon
   * @param <T> type
   */
  public record NodeSearchContext<T>(int level, boolean includeParent, CacheSnapshot<T> cacheSnapshot) {
    /**
     * @return node context for the next level of iteration
     */
    public NodeSearchContext<T> next() {
      return new NodeSearchContext<>(this.level + 1, this.includeParent, this.cacheSnapshot);
    }
  }

  /**
   * @param child to add
   * @param cacheSnapshot snapshot of a specific version of the cache to populate
   * @param parentKey to determine parent node
   * @param <T> type
   */
  public record NodePopulationContext<T>(Node<T> child, CacheSnapshot<T> cacheSnapshot, String parentKey) {}
}