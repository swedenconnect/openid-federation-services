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
import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.tree.Node;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implements the operations towards redis using typed keys.
 *
 * @author Felix Hellman
 */
public class RedisOperations {

  /**
   * Constructor.
   * @param entityTemplate for handling entity statements
   * @param childTemplate for handling child listings
   */
  public RedisOperations(final RedisTemplate<String, EntityStatement> entityTemplate,
      final RedisTemplate<String, String> childTemplate) {
    this.template = entityTemplate;
    this.stringTemplate = childTemplate;
  }

  private final RedisTemplate<String, EntityStatement> template;
  private final RedisTemplate<String, String> stringTemplate;

  /**
   * Gets all children for an entity
   * empty list if key does not exist or if the key has no children.
   * @param childKey to search for
   * @return list of children, empty list if no children
   */
  public List<Node<EntityStatement>> getChildren(final ChildKey childKey) {
    final Set<String> members = this.stringTemplate.opsForSet().members(childKey.getRedisKey());
    if (Objects.nonNull(members)) {
      return members
          .stream()
          .map(key -> new Node<EntityStatement>(NodeKey.parse(key)))
          .toList();
    }
    return List.of();
  }

  /**
   * Appends a child to a parent
   * @param parent key
   * @param child node key
   */
  public void append(final ChildKey parent, final Node<EntityStatement> child) {
    this.stringTemplate.opsForSet().add(parent.getRedisKey(), child.getKey().getKey());
  }

  /**
   * Sets or updates value for key
   * @param key to update for
   * @param data to set
   */
  public void setData(final EntityKey key, final EntityStatement data) {
    this.template.opsForValue().set(key.getRedisKey(), data);
  }

  /**
   * Gets data for key
   * @param key for value
   * @return value, can be null
   */
  public EntityStatement getData(final EntityKey key) {
    return this.template.opsForValue().get(key.getRedisKey());
  }

  /**
   * Gets root node
   * @param key for root
   * @return root node
   */
  public Node<EntityStatement> getRoot(final RootKey key) {
    final String root = this.stringTemplate.opsForValue().get(key.getRedisKey());
    return new Node<>(NodeKey.parse(root));
  }

  /**
   * Sets root node for a given tree.
   * @param key for the root node
   * @param root node key for root
   */
  public void setRoot(final RootKey key, final Node<EntityStatement> root) {
    this.stringTemplate.opsForValue().set(key.getRedisKey(), root.getKey().getKey());
  }

  /**
   * Key for handling entities.
   * @param location internal node key
   * @param version to which the entity belongs to
   * @param alias to which module the data belongs to
   */
  public record EntityKey(String location, int version, String alias) {
    String getRedisKey() {
      return "%s:%d:entity:%s".formatted(this.alias, this.version, this.location);
    }
  }

  /**
   * Key for handling child listings.
   * @param parent to which the child belongs to
   * @param version to which the child and parent belongs to
   * @param alias to which module the data belongs to
   */
  public record ChildKey(Node<EntityStatement> parent, int version, String alias) {
    String getRedisKey() {
      return "%s:%d:children:%s".formatted(this.alias, this.version, this.parent.getKey());
    }
  }

  /**
   * Key for handling root nodes.
   * @param version to which the root belongs to
   * @param alias to which module the data belongs to
   */
  public record RootKey(int version, String alias) {
    String getRedisKey() {
      return "%s:%d:root".formatted(this.alias, this.version);
    }
  }
}
