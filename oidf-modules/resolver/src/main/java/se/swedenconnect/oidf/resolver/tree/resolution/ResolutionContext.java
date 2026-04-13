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
package se.swedenconnect.oidf.resolver.tree.resolution;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for resolver resolution.
 *
 * @author Felix Hellman
 */
@NoArgsConstructor
public class ResolutionContext {
  private final Set<String> visited = ConcurrentHashMap.newKeySet();
  @Getter
  @Setter
  private EntityStatementWrapper trustAnchorEntityStatement;

  /**
   * Adds an entity
   * @param entity to add
   * @return true if the entity didn't already exist, otherwise false
   */
  public boolean add(final String entity) {
    return this.visited.add(entity);
  }

  /**
   * Adds entities to context
   * @param toAdd to add to context
   * @return all unseen entities
   */
  public Set<String> addEnteties(final List<String> toAdd) {
    final Set<String> addedEntities = new HashSet<>();
    toAdd.stream().forEach(entity -> {
      if (this.visited.add(entity)) {
        addedEntities.add(entity);
      }
    });
    return addedEntities;
  }
}
