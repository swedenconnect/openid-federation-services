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
package se.swedenconnect.oidf.common.entity.tree;

import java.util.function.BiPredicate;

/**
 * Search requests towards a tree.
 * @param predicate to find matching nodes for
 * @param includeParent true if the result should include all level of parents, false to only include matches.
 * @param snapshot version of the tree
 * @param stopOnFirstMatch true if sibling subtrees should be skipped once any child subtree returns a match.
 *                         Use for searches targeting a unique entity (e.g. resolve, entity lookup).
 *                         Leave false for exhaustive searches (e.g. discovery).
 * @param <T> type of entity
 *
 * @author Felix Hellman
 */
public record SearchRequest<T>(
    BiPredicate<T, Node.NodeSearchContext<T>> predicate,
    boolean includeParent,
    CacheSnapshot<T> snapshot,
    boolean stopOnFirstMatch) {

  /**
   * Convenience constructor that defaults {@code stopOnFirstMatch} to {@code false}.
   */
  public SearchRequest(
      final BiPredicate<T, Node.NodeSearchContext<T>> predicate,
      final boolean includeParent,
      final CacheSnapshot<T> snapshot) {
    this(predicate, includeParent, snapshot, false);
  }
}
