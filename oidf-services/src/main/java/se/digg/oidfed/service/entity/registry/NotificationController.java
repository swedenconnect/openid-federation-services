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
package se.digg.oidfed.service.entity.registry;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import se.digg.oidfed.common.entity.integration.registry.RegistryVerifier;

@RestController
public class NotificationController {

  private final ApplicationEventPublisher publisher;
  private final RegistryVerifier registryVerifier;

  public NotificationController(final ApplicationEventPublisher publisher, final RegistryVerifier registryVerifier) {
    this.publisher = publisher;
    this.registryVerifier = registryVerifier;
  }

  @PostMapping("/registry/notify")
  public void notify(final String body) {
    //TODO verify
    this.publisher.publishEvent(new RegistryNotificationEvent());
  }
}
