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

import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implements the operations towards redis using typed keys.
 *
 * @author Felix Hellman
 */
public class ResolverRedisOperations {

  /**
   * Constructor.
   * @param entityTemplate for handling entity statements
   * @param childTemplate for handling child listings
   * @param cacheTtl time-to-live for cached entries
   */
  public ResolverRedisOperations(final RedisTemplate<String, ScrapedEntity> entityTemplate,
                                 final RedisTemplate<String, String> childTemplate,
                                 final Duration cacheTtl) {
    this.template = entityTemplate;
    this.stringTemplate = childTemplate;
    this.cacheTtl = cacheTtl;
  }

  private final RedisTemplate<String, ScrapedEntity> template;
  private final RedisTemplate<String, String> stringTemplate;
  private final Duration cacheTtl;

  /**
   * Gets all children for an entity
   * empty list if key does not exist or if the key has no children.
   * @param childKey to search for
   * @return list of children, empty list if no children
   */
  public List<Node<ScrapedEntity>> getChildren(final ChildKey childKey) {
    final Set<String> members = this.stringTemplate.opsForSet().members(childKey.getRedisKey());
    if (Objects.nonNull(members)) {
      return members
          .stream()
          .map(key -> new Node<ScrapedEntity>(NodeKey.parse(decode(key))))
          .toList();
    }
    return List.of();
  }

  /**
   * Appends a child to a parent
   * @param parent key
   * @param child node key
   */
  public void append(final ChildKey parent, final Node<ScrapedEntity> child) {
    this.stringTemplate.opsForSet().add(parent.getRedisKey(), encode(child.getKey().getKey()));
    this.stringTemplate.expire(parent.getRedisKey(), this.cacheTtl);
  }

  /**
   * Sets or updates value for key
   * @param key to update for
   * @param data to set
   */
  public void setData(final EntityKey key, final ScrapedEntity data) {
    this.template.<String, ScrapedEntity>opsForHash().put(key.getHashKey(), key.getHashField(), data);
    this.template.expire(key.getHashKey(), this.cacheTtl);
  }

  /**
   * Gets data for key
   * @param key for value
   * @return value, can be null
   */
  public ScrapedEntity getData(final EntityKey key) {
    return this.template.<String, ScrapedEntity>opsForHash().get(key.getHashKey(), key.getHashField());
  }

  /**
   * Gets root node
   * @param key for root
   * @return root node
   */
  public Node<ScrapedEntity> getRoot(final RootKey key) {
    final String root = this.stringTemplate.opsForValue().get(key.getRedisKey());
    if (root == null) {
      return null;
    }
    return new Node<>(NodeKey.parse(decode(root)));
  }

  /**
   * Sets root node for a given tree.
   * @param key for the root node
   * @param root node key for root
   */
  public void setRoot(final RootKey key, final Node<ScrapedEntity> root) {
    this.stringTemplate.opsForValue().set(key.getRedisKey(), encode(root.getKey().getKey()));
    this.stringTemplate.expire(key.getRedisKey(), this.cacheTtl);
  }

  /**
   * Key for handling entities stored in a hash.
   * @param location internal node key (used as hash field)
   * @param version to which the entity belongs to
   * @param entityId to which module the data belongs to
   */
  public record EntityKey(String location, long version, String entityId) {
    String getHashKey() {
      return "%s:%d:entities".formatted(encode(this.entityId), this.version);
    }

    String getHashField() {
      return encode(this.location);
    }
  }

  /**
   * Key for handling child listings.
   * @param parent to which the child belongs to
   * @param version to which the child and parent belongs to
   * @param entityId to which module the data belongs to
   */
  public record ChildKey(Node<ScrapedEntity> parent, long version, String entityId) {
    String getRedisKey() {
      return "%s:%d:children:%s".formatted(encode(this.entityId), this.version, encode(this.parent.getKey().getKey()));
    }
  }

  /**
   * Key for handling root nodes.
   * @param version to which the root belongs to
   * @param entityId to which module the data belongs to
   */
  public record RootKey(long version, String entityId) {
    String getRedisKey() {
      return "%s:%d:root".formatted(encode(this.entityId), this.version);
    }
  }

  static String encode(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  static String decode(final String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
