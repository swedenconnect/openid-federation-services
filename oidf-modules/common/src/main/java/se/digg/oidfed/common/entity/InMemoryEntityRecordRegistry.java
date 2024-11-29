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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * In memory implementation of {@link EntityRecordRegistry}
 *
 * @author Felix Hellman
 */
public class InMemoryEntityRecordRegistry implements EntityRecordRegistry {
  private final Map<String, EntityRecord> pathEntities = new HashMap<>();
  private final Map<EntityID, EntityRecord> idEntityRecordMap = new HashMap<>();
  private final String basePath;

  /**
   * @param basePath for this registry
   */
  public InMemoryEntityRecordRegistry(final String basePath) {
    this.basePath = basePath;
  }

  @Override
  public Optional<EntityRecord> getEntity(final String path) {
    return Optional.ofNullable(this.pathEntities.get(path));
  }

  @Override
  public Set<String> getPaths() {
    return this.pathEntities.keySet();
  }

  @Override
  public Optional<EntityRecord> getEntity(final EntityID entityID) {
    return Optional.ofNullable(this.idEntityRecordMap.get(entityID));
  }

  @Override
  public void addEntity(final EntityRecord record) {
    this.idEntityRecordMap.put(record.getSubject(), record);

    Optional.ofNullable(record.getHostedRecord()).ifPresent(hostedRecord -> {
      this.pathEntities.put(EntityPathFactory.getPath(record, this.basePath), record);
    });
  }

  @Override
  public String getBasePath() {
    return this.basePath;
  }

  @Override
  public List<EntityRecord> find(final Predicate<EntityRecord> predicate) {
    return this.idEntityRecordMap.values().stream().filter(predicate).toList();
  }
}
