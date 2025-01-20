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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.health.ReadyStateComponent;

import java.util.List;

/**
 * Endpoint that can tell a loadbalancer if it is ready for traffic or not.
 *
 * @author Felix Hellman
 */
@Component
@Endpoint(id = "ready")
@Slf4j
public class ApplicationReadyEndpoint {

  private final List<ReadyStateComponent> readyStateComponents;

  /**
   * Constructor.
   *
   * @param readyStateComponents to load
   */
  public ApplicationReadyEndpoint(final List<ReadyStateComponent> readyStateComponents) {
    this.readyStateComponents = readyStateComponents;
  }

  /**
   * @return true if all modules have been loaded.
   */
  @ReadOperation
  public boolean applicationReady() {
    final long nonReadyComponents = this.readyStateComponents
        .stream()
        .filter(c -> !c.ready())
        .count();
    return nonReadyComponents == 0;
  }
}
