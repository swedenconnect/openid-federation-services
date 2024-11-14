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
package se.digg.oidfed.service.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.entity.EntityRegistry;

import java.util.List;
import java.util.Optional;

class EntityRegistryTest {

  @Test
  void registryPopulatesAndIsSearchable() {
    final EntityProperties second = new EntityProperties();
    final String basePath = "http://entity-identifier.test";

    second.setAlias("second");
    second.setEntityIdentifier(basePath + "/root/");

    final EntityProperties first = new EntityProperties();
    first.setAlias("root");
    first.setChildren(List.of(second.getEntityIdentifier()));
    first.setEntityIdentifier(basePath + "/root");
    first.setIsRoot(true);
    final EntityRegistry registry = new EntityRegistry(List.of(first, second));
    final Optional<EntityProperties> entity = registry.getEntity("/root/second");

    Assertions.assertTrue(entity.isPresent());
    Assertions.assertEquals(second.getEntityIdentifier(), entity.get().getEntityIdentifier());
  }
}