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
package se.swedenconnect.oidf.service.resolver.cache;

import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.ResolverCache;
import se.swedenconnect.oidf.common.entity.tree.SnapshotSource;
import se.swedenconnect.oidf.common.entity.tree.VersionedCacheLayer;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.List;
import java.util.Optional;

/**
 * Redis implementation of {@link VersionedCacheLayer} and {@link SnapshotSource}
 *
 * @author Felix Hellman
 */
public class RedisVersionedCacheLayer implements ResolverCache {
  private final RedisTemplate<String, Long> versionTemplate;

  private final ResolverRedisOperations resolverRedisOperations;
  private final ResolverProperties properties;

  /**
   * Constructor.
   * @param versionTemplate for handling version numbers
   * @param resolverRedisOperations for handling data operations upon a tree
   * @param properties for handling which submodule the operation is for
   */
  public RedisVersionedCacheLayer(
      final RedisTemplate<String, Long> versionTemplate,
      final ResolverRedisOperations resolverRedisOperations,
      final ResolverProperties properties
      ) {

    this.versionTemplate = versionTemplate;
    this.resolverRedisOperations = resolverRedisOperations;
    this.properties = properties;
  }

  @Override
  public List<Node<ScrapedEntity>> getChildren(final Node<ScrapedEntity> parent, final long version) {
    final ScrapedEntity parentData = this.getData(parent.getKey().getKey(), version);
    if (parentData != null && parentData.getIntermediate() != null) {
      return parentData.getIntermediate().subordinates().keySet().stream()
          .map(key -> new Node<ScrapedEntity>(new NodeKey(key, key)))
          .toList();
    }
    return List.of();
  }

  @Override
  public void append(final Node<ScrapedEntity> child, final Node<ScrapedEntity> parent, final long version) {
    this.resolverRedisOperations
        .append(new ResolverRedisOperations.ChildKey(parent, version, this.properties.getEntityIdentifier()), child);
  }

  @Override
  public void setData(final String location, final ScrapedEntity data, final long version) {
    this.resolverRedisOperations.
        setData(new ResolverRedisOperations.EntityKey(location, version, this.properties.getEntityIdentifier()), data);
  }

  @Override
  public ScrapedEntity getData(final String location, final long version) {
    return this.resolverRedisOperations
        .getData(new ResolverRedisOperations.EntityKey(location, version, this.properties.getEntityIdentifier()));
  }

  @Override
  public Node<ScrapedEntity> getRoot(final long version) {
    return this.resolverRedisOperations
        .getRoot(new ResolverRedisOperations.RootKey(version, this.properties.getEntityIdentifier()));
  }

  @Override
  public long getCurrentVersion() {
    final BoundValueOperations<String, Long> stringLongBoundValueOperations =
        this.versionTemplate.boundValueOps("%s:tree:version".formatted(this.properties.getEntityIdentifier()));
    return Optional.ofNullable(stringLongBoundValueOperations.get()).orElse(0L);
  }

  @Override
  public void useNextVersion() {
    final Long pendingVersion = this.versionTemplate
        .boundValueOps("%s:tree:pending-version".formatted(this.properties.getEntityIdentifier()))
        .get();
    if (pendingVersion != null) {
      this.versionTemplate
          .boundValueOps("%s:tree:version".formatted(this.properties.getEntityIdentifier()))
          .set(pendingVersion);
    }
  }

  @Override
  public CacheSnapshot<ScrapedEntity> snapshot() {
    return new CacheSnapshot<>(this, this.getCurrentVersion());
  }

  @Override
  public CacheSnapshot<ScrapedEntity> createNewSnapshot(final Node<ScrapedEntity> root,
                                                        final ScrapedEntity rootData) {
    final long version = getNextVersion();
    this.versionTemplate
        .boundValueOps("%s:tree:pending-version".formatted(this.properties.getEntityIdentifier()))
        .set(version);
    this.resolverRedisOperations
        .setRoot(
            new ResolverRedisOperations.RootKey(version, this.properties.getEntityIdentifier()), root
        );
    this.setData(root.getKey().getKey(), rootData, version);
    return new CacheSnapshot<>(this, version);
  }
}
