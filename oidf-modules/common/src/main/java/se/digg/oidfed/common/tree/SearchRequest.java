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

import java.util.function.BiPredicate;

/**
 * Search requests towards a tree.
 * @param predicate to find matching nodes for
 * @param includeParent true if the result should include all level of parents, false to only include matches.
 * @param snapshot version of the tree
 * @param <T> type of entity
 *
 * @author Felix Hellman
 */
public record SearchRequest<T>(
    BiPredicate<T, Node.NodeSearchContext<T>> predicate,
    boolean includeParent,
    CacheSnapshot<T> snapshot) {
}
