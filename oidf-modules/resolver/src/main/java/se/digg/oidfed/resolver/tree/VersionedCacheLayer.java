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

import java.util.List;

/**
 * Underlying cache layer that handles the tree data.
 * @param <T> type of entity data
 *
 * @author Felix Hellman
 */
public interface VersionedCacheLayer<T> {
  /**
   * Returns a list of children for a given node.
   * @param parent node to get children from
   * @param version index of which tree to operate upon
   * @return list of children (keys)
   */
  List<Node<T>> getChildren(final Node<T> parent, final int version);

  /**
   * Adds a child node (key) to a parent.
   * @param child to add
   * @param parent subject of the addition
   * @param version index of which tree to operate upon
   */
  void append(final Node<T> child, final Node<T> parent, final int version);

  /**
   * Sets the value of a given entity.
   * @param key of the entity
   * @param data of the entity
   * @param version index of which tree to operate upon
   */
  void setData(final String key, final T data, final int version);

  /**
   * Gets the root node for a given tree.
   * @param version index of which tree to operate upon
   * @return root node
   */
  Node<T> getRoot(int version);

  /**
   * Gets the entity from a key.
   * @param key of the entity
   * @param version index of which tree to operate upon
   * @return the entity
   */
  T getData(final String key, final int version);

  /**
   * @return the current tree index
   */
  int getCurrentVersion();

  /**
   * @return next tree index
   */
  default int getNextVersion() {
    return this.getCurrentVersion() + 1 % 100;
  }

  /**
   * Moves current version index to next version index
   */
  void useNextVersion();
}