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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * EntityRecordRegistry that adds quality of life features to an existing {@link EntityRecordRegistry}.
 *
 * @author Felix Hellman
 */
public class DelegatingEntityRecordRegistry implements EntityRecordRegistry {
  private final EntityID defaultEntity;
  private final EntityRecordRegistry registry;
  private final List<Consumer<EntityRecord>> entityRecordRegistrationHook;

  @Override
  public Optional<EntityRecord> getEntity(final String path) {
    if (Objects.isNull(path) || path.isEmpty() || path.equalsIgnoreCase("/")) {
      return this.registry.getEntity(this.defaultEntity);
    }
    return this.registry.getEntity(path);
  }

  @Override
  public Set<String> getPaths() {
    return this.registry.getPaths();
  }

  @Override
  public Optional<EntityRecord> getEntity(final EntityID entityID) {
    return this.registry.getEntity(entityID);
  }

  /**
   * @param defaultEntity to find under the "/" or empty path
   * @param registry to perform serach upon
   * @param entityRecordRegistrationHook to execute when a record has been added
   */
  public DelegatingEntityRecordRegistry(
      final EntityID defaultEntity,
      final EntityRecordRegistry registry,
      final List<Consumer<EntityRecord>> entityRecordRegistrationHook) {

    this.defaultEntity = defaultEntity;
    this.registry = registry;
    this.entityRecordRegistrationHook = entityRecordRegistrationHook;
  }

  @Override
  public void addEntity(final EntityRecord record) {
    this.registry.addEntity(record);
    this.entityRecordRegistrationHook.forEach(hook -> hook.accept(record));
  }

  @Override
  public String getBasePath() {
    return this.registry.getBasePath();
  }

  @Override
  public List<EntityRecord> find(final Predicate<EntityRecord> predicate) {
    return this.registry.find(predicate);
  }
}
