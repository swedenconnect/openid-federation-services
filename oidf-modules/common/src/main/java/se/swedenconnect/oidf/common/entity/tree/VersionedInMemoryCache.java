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

import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In memory implementation of {@link ResolverCache}
 *
 * @author Felix Hellman
 */
public class VersionedInMemoryCache implements ResolverCache {

  private final Map<String, List<Node<ScrapedEntity>>> childMap = new ConcurrentHashMap<>();
  private final Map<String, ScrapedEntity> dataMap = new ConcurrentHashMap<>();
  private final Map<Integer, Node<ScrapedEntity>> rootMap = new ConcurrentHashMap<>();

  private final AtomicInteger integer = new AtomicInteger(0);

  @Override
  public void setData(final String key, final ScrapedEntity data, final int version) {
    this.dataMap.put(this.getKey(key, version), data);
  }

  @Override
  public Node<ScrapedEntity> getRoot(final int version) {
    return this.rootMap.get(version);
  }

  @Override
  public ScrapedEntity getData(final String key, final int version) {
    return this.dataMap.get(this.getKey(key, version));
  }

  private String getKey(final String key, final int version) {
    return "%d:%s".formatted(version, key);
  }

  @Override
  public List<Node<ScrapedEntity>> getChildren(final Node<ScrapedEntity> node, final int version) {
    return Optional.ofNullable(this.childMap.get(this.getKey(node.getKey().getKey(), version))).orElseGet(List::of);
  }

  @Override
  public synchronized void append(
      final Node<ScrapedEntity> child, final Node<ScrapedEntity> parent,
      final int version) {
    //Can probably be solved without synchronized using compute if missing ...
    List<Node<ScrapedEntity>> nodes = this.childMap.get(this.getKey(parent.getKey().getKey(), version));
    if (Objects.isNull(nodes)) {
      nodes = new ArrayList<>();
    }
    nodes.add(child);
    this.childMap.put(this.getKey(parent.getKey().getKey(), version), nodes);
  }

  @Override
  public int getCurrentVersion() {
    return this.integer.get();
  }

  @Override
  public void useNextVersion() {
    this.integer.set(this.getNextVersion());
  }

  @Override
  public CacheSnapshot<ScrapedEntity> snapshot() {
    return new CacheSnapshot<>(this, this.getCurrentVersion());
  }

  @Override
  public CacheSnapshot<ScrapedEntity> createNewSnapshot(
      final Node<ScrapedEntity> root,
      final ScrapedEntity rootData) {
    final int version = this.getNextVersion();
    this.rootMap.put(version, root);
    this.setData(root.getKey().getKey(), rootData, version);
    return new CacheSnapshot<>(this, version);
  }
}
