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
package se.digg.oidfed.service.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.registry.RefreshAheadRecordRegistrySource;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.modules.TrustMarkIssuerInitializedEvent;

/**
 * Initializer class for entity registry.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class EntityInitializer extends ReadyStateComponent {

  private final EntityRouter router;
  private final EntityRecordRegistry registry;
  private final RefreshAheadRecordRegistrySource source;

  /**
   * @param router to use
   * @param registry to use
   * @param source to use
   */
  public EntityInitializer(
      final EntityRouter router,
      final EntityRecordRegistry registry,
      final RefreshAheadRecordRegistrySource source) {
    this.router = router;
    this.registry = registry;
    this.source = source;
  }

  /**
   * @param event to handle
   * @return event to notify the system that entities has been loaded
   */
  @EventListener
  public EntitiesLoadedEvent handle(final TrustMarkIssuerInitializedEvent event) {
    this.source.getAllEntities().forEach(this.registry::addEntity);
    this.router.reevaluteEndpoints();
    this.markReady();
    return new EntitiesLoadedEvent();
  }

  @Override
  public String name() {
    return "entity-init";
  }
}
