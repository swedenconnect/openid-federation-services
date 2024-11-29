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

import java.util.List;

/**
 * Cache snapshot of underlying {@link VersionedCacheLayer} to lock in a specific version.
 *
 * @param <T> Datatype for cache
 *
 * @author Felix Hellman
 */
public class CacheSnapshot<T> {
  private final VersionedCacheLayer<T> cache;
  private final int version;

  /**
   * Constructor.
   * @param cache to perform operations upon
   * @param version key to use with the cache
   */
  public CacheSnapshot(final VersionedCacheLayer<T> cache, final int version) {
    this.cache = cache;
    this.version = version;
  }

  /**
   * @param key for data
   * @return data for a given key, can be null
   */
  public T getData(final String key) {
    return this.cache.getData(key,this.version);
  }

  /**
   * @param key for data
   * @param data to add to key
   */
  public void setData(final String key, final T data) {
    this.cache.setData(key, data,this.version);
  }

  /**
   * Adds a child node to a parent node.
   * @param child to add
   * @param parent to add the child to
   */
  public void append(final Node<T> child, final Node<T> parent) {
    this.cache.append(child, parent, this.version);
  }

  /**
   * @param parent to list children of
   * @return list of children
   */
  public List<Node<T>> getChildren(final Node<T> parent) {
    return this.cache.getChildren(parent,this.version);
  }

  /**
   * @return root node of the given tree
   */
  public Node<T> getRoot() {
    return this.cache.getRoot(this.version);
  }
}
