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
  private final EntityRecordRegistry registry;
  private final List<Consumer<EntityRecord>> entityRecordRegistrationHook;

  @Override
  public Optional<EntityRecord> getEntity(final String path) {
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
   * @param registry to perform serach upon
   * @param entityRecordRegistrationHook to execute when a record has been added
   */
  public DelegatingEntityRecordRegistry(
      final EntityRecordRegistry registry,
      final List<Consumer<EntityRecord>> entityRecordRegistrationHook) {
    this.registry = registry;
    this.entityRecordRegistrationHook = entityRecordRegistrationHook;
  }

  @Override
  public void addEntity(final EntityRecord record) {
    this.registry.addEntity(record);
    this.entityRecordRegistrationHook.forEach(hook -> hook.accept(record));
  }

  @Override
  public List<EntityRecord> find(final Predicate<EntityRecord> predicate) {
    return this.registry.find(predicate);
  }
}
