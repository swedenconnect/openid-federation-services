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
package se.digg.oidfed.common.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.tree.CacheSnapshot;
import se.digg.oidfed.common.tree.Node;
import se.digg.oidfed.common.tree.SearchRequest;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.common.tree.VersionedInMemoryCache;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Registry of entities.
 *
 * @author Felix Hellman
 */
public class EntityRegistry {

  private final Map<String, EntityProperties> pathedEntities = new HashMap<>();

  private final EntityProperties root;

  /**
   * @param path of the entity e.g. /root/second
   * @return property of entity
   */
  public Optional<EntityProperties> getEntity(final String path) {
    if (Objects.isNull(path) || path.isEmpty() || path.equalsIgnoreCase("/")) {
      return Optional.of(root);
    }
    return Optional.ofNullable(pathedEntities.get(path));
  }

  /**
   * @return paths mapped in the entity registry
   */
  public Set<String> getPaths() {
    return pathedEntities.keySet();
  }

  /**
   * @param entityID of the entity. e.g. http://myentity.test
   * @return property of entity
   */
  public Optional<EntityProperties> getEntity(final EntityID entityID) {
    return pathedEntities.values()
        .stream()
        .filter(v -> v.getEntityIdentifier().equalsIgnoreCase(entityID.getValue()))
        .findFirst();
  }

  /**
   * Constructor.
   * Creates a temporary tree for creating path mappings.
   *
   * @param entityProperties to initialize the registry with
   *
   */
  public EntityRegistry(final List<EntityProperties> entityProperties) {
    final Tree<EntityProperties> propertiesTree = new Tree<>(new VersionedInMemoryCache<>());
    final EntityProperties root = entityProperties.stream()
        .filter(EntityProperties::getIsRoot)
        .findFirst()
        .orElseThrow();

    final CacheSnapshot<EntityProperties> snapshot =
        propertiesTree.addRoot(new Node<>(root.getEntityIdentifier()), root);

    this.root = root;

    final ArrayDeque<String> queue = new ArrayDeque<>(root.getChildren());

    while (!queue.isEmpty()) {
      final String child = queue.pollFirst();
      final Optional<EntityProperties> entity = find(child, entityProperties);
      entity.ifPresent(e -> {
        propertiesTree.addChild(new Node<>(child), root.getEntityIdentifier(), e, snapshot);
        Optional.ofNullable(e.getChildren()).ifPresent(queue::addAll);
      });
    }

    entityProperties.forEach(pr -> addEntity(propertiesTree, pr, snapshot));

  }

  private Optional<EntityProperties> find(final String entityId, final List<EntityProperties> entityProperties) {
    return entityProperties.stream()
        .filter(f -> f.getEntityIdentifier().equalsIgnoreCase(entityId))
        .findFirst();
  }

  private void addEntity(final Tree<EntityProperties> tree, final EntityProperties properties,
      final CacheSnapshot<EntityProperties> snapshot) {
    final SearchRequest<EntityProperties> request =
        new SearchRequest<>((ep, c) -> ep.getEntityIdentifier().equalsIgnoreCase(properties.getEntityIdentifier()),
            true, snapshot);
    final String path = tree.search(request)
        .stream()
        .sorted(Comparator.comparingInt(a -> a.context().level()))
        .map(ep -> ep.getData().getAlias())
        .reduce("", (a, b) -> a + "/" + b);

    pathedEntities.put(path, properties);
  }
}
