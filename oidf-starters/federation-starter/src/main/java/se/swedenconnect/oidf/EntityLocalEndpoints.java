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
package se.swedenconnect.oidf;

import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.Optional;

/**
 * The Entity Configuration endpoints of the hosted entities, answered the same way as {@code EntityRouter} answers
 * them over HTTP.
 *
 * @author Felix Hellman
 */
public class EntityLocalEndpoints implements LocalFederationEndpoints {

  private final CompositeRecordSource source;
  private final EntityConfigurationFactory factory;

  /**
   * Constructor.
   *
   * @param source  of the hosted entities
   * @param factory for the Entity Configurations
   */
  public EntityLocalEndpoints(final CompositeRecordSource source, final EntityConfigurationFactory factory) {
    this.source = source;
    this.factory = factory;
  }

  @Override
  public Optional<String> entityConfiguration(final String entityId) {
    return this.source.getEntity(new NodeKey(entityId))
        .map(entity -> this.factory.createEntityConfiguration(entity).serialize());
  }
}
