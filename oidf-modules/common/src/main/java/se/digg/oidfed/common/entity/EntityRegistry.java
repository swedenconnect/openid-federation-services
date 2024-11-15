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
    final EntityProperties root = entityProperties.stream()
        .filter(EntityProperties::getIsRoot)
        .findFirst()
        .orElseThrow();

    this.root = root;

    entityProperties.forEach(ep -> this.pathedEntities.put(ep.getPath(), ep));
  }
}
