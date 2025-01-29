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
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Actuator endpoint for triggering reloads from entity record registry.
 *
 * @author Felix Hellman
 */
@Component
@Endpoint(id = "entityreload")
@Slf4j
public class EntityReloadEndpoint {

  /**
   * Constructor.
   * @param publisher for publishing events
   */
  public EntityReloadEndpoint(final ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  private final ApplicationEventPublisher publisher;

  @WriteOperation
  void reloadFromRegistry() {
    log.info("Entity reload triggered externally.");
    this.publisher.publishEvent(new EntityReloadEvent());
  }
}
