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
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
   * @param key of the entity. e.g. "http://myentity.test:http://myentity.test"
   * @return property of entity
   */
  Optional<EntityRecord> getEntity(final NodeKey key);
  /**
   * @param record to add
   */
  void addEntity(final EntityRecord record);


  /**
   * Search for subordinates for a given issuer
   * @param issuer to search for
   * @return list of subordinates for a given issuer
   */
  List<EntityRecord> findSubordinates(final String issuer);
}
