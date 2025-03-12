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
package se.digg.oidfed.service.resolver.cache;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.tree.CacheSnapshot;
import se.digg.oidfed.common.tree.Node;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.common.tree.SnapshotSource;
import se.digg.oidfed.common.tree.VersionedCacheLayer;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;

import java.util.List;
import java.util.Optional;

/**
 * Redis implementation of {@link VersionedCacheLayer} and {@link SnapshotSource}
 *
 * @author Felix Hellman
 */
public class RedisVersionedCacheLayer implements ResolverCache {
  private final RedisTemplate<String, Integer> versionTemplate;

  private final ResolverRedisOperations resolverRedisOperations;
  private final ResolverProperties properties;

  /**
   * Constructor.
   * @param versionTemplate for handling version numbers
   * @param resolverRedisOperations for handling data operations upon a tree
   * @param properties for handling which submodule the operation is for
   */
  public RedisVersionedCacheLayer(
      final RedisTemplate<String, Integer> versionTemplate,
      final ResolverRedisOperations resolverRedisOperations,
      final ResolverProperties properties
      ) {

    this.versionTemplate = versionTemplate;
    this.resolverRedisOperations = resolverRedisOperations;
    this.properties = properties;
  }

  @Override
  public List<Node<EntityStatement>> getChildren(final Node<EntityStatement> parent, final int version) {
    return this.resolverRedisOperations
        .getChildren(new ResolverRedisOperations.ChildKey(parent, version, this.properties.entityIdentifier()));
  }

  @Override
  public void append(final Node<EntityStatement> child, final Node<EntityStatement> parent, final int version) {
    this.resolverRedisOperations
        .append(new ResolverRedisOperations.ChildKey(parent, version, this.properties.entityIdentifier()), child);
  }

  @Override
  public void setData(final String location, final EntityStatement data, final int version) {
    this.resolverRedisOperations.
        setData(new ResolverRedisOperations.EntityKey(location, version, this.properties.entityIdentifier()), data);
  }

  @Override
  public EntityStatement getData(final String location, final int version) {
    return this.resolverRedisOperations
        .getData(new ResolverRedisOperations.EntityKey(location, version, this.properties.entityIdentifier()));
  }

  @Override
  public Node<EntityStatement> getRoot(final int version) {
    return this.resolverRedisOperations.getRoot(new ResolverRedisOperations.RootKey(version, this.properties.entityIdentifier()));
  }

  @Override
  public int getCurrentVersion() {
    final BoundValueOperations<String, Integer> stringIntegerBoundValueOperations =
        this.versionTemplate.boundValueOps("%s:tree:version".formatted(this.properties.entityIdentifier()));
    return Optional.ofNullable(stringIntegerBoundValueOperations.get()).orElse(0);
  }

  @Override
  public void useNextVersion() {
    this.versionTemplate.boundValueOps("%s:tree:version".formatted(this.properties.entityIdentifier())).set(getNextVersion());
  }

  @Override
  public CacheSnapshot<EntityStatement> snapshot() {
    return new CacheSnapshot<>(this, this.getCurrentVersion());
  }

  @Override
  public CacheSnapshot<EntityStatement> createNewSnapshot(final Node<EntityStatement> root,
      final EntityStatement rootData) {
    final int version = getNextVersion();
    this.resolverRedisOperations.setRoot(new ResolverRedisOperations.RootKey(version, this.properties.entityIdentifier()), root);
    this.setData(root.getKey().getKey(), rootData, version);
    return new CacheSnapshot<>(this, version);
  }
}
