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
package se.digg.oidfed.common.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

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

  private final Map<String, List<Node<EntityStatement>>> childMap = new ConcurrentHashMap<>();
  private final Map<String, EntityStatement> dataMap = new ConcurrentHashMap<>();
  private final Map<Integer, Node<EntityStatement>> rootMap = new ConcurrentHashMap<>();

  private final AtomicInteger integer = new AtomicInteger(0);

  @Override
  public void setData(final String key, final EntityStatement data, final int version) {
    this.dataMap.put(this.getKey(key, version), data);
  }

  @Override
  public Node<EntityStatement> getRoot(final int version) {
    return this.rootMap.get(version);
  }

  @Override
  public EntityStatement getData(final String key, final int version) {
    return this.dataMap.get(this.getKey(key, version));
  }

  private String getKey(final String key, final int version) {
    return "%d:%s".formatted(version, key);
  }

  @Override
  public List<Node<EntityStatement>> getChildren(final Node<EntityStatement> node, final int version) {
    return Optional.ofNullable(this.childMap.get(this.getKey(node.getKey().getKey(), version))).orElseGet(List::of);
  }

  @Override
  public synchronized void append(
      final Node<EntityStatement> child, final Node<EntityStatement> parent,
      final int version) {
    //Can probably be solved without synchronized using compute if missing ...
    List<Node<EntityStatement>> nodes = this.childMap.get(this.getKey(parent.getKey().getKey(), version));
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
  public CacheSnapshot<EntityStatement> snapshot() {
    return new CacheSnapshot<>(this, this.getCurrentVersion());
  }

  @Override
  public CacheSnapshot<EntityStatement> createNewSnapshot(
      final Node<EntityStatement> root,
      final EntityStatement rootData) {
    final int version = this.getNextVersion();
    this.rootMap.put(version, root);
    this.setData(root.getKey().getKey(), rootData, version);
    return new CacheSnapshot<>(this, version);
  }
}
