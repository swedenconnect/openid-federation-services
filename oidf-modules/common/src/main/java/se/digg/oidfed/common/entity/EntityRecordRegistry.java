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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interface to implement for persisting entity records.
 *
 * @author Felix Hellman
 */
public interface EntityRecordRegistry {
  /**
   * @param path of the entity e.g. /root/second
   * @return property of entity
   */
  Optional<EntityRecord> getEntity(final String path);

  /**
   * @return paths mapped in the entity registry
   */
  Set<String> getPaths();

  /**
   * @param entityID of the entity. e.g. http://myentity.test
   * @return property of entity
   */
  Optional<EntityRecord> getEntity(final EntityID entityID);

  /**
   * @param record to add
   */
  void addEntity(final EntityRecord record);

  /**
   * @return base path of this registry
   */
  String getBasePath();

  /**
   * Takes a predicate to find entities.
   * @param predicate to use
   * @return list of hits
   */
  List<EntityRecord> find(final Predicate<EntityRecord> predicate);
}
