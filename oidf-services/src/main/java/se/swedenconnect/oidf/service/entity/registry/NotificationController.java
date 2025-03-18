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
package se.swedenconnect.oidf.service.entity.registry;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.common.entity.entity.RecordVerificationException;
import se.swedenconnect.oidf.common.entity.entity.integration.CacheRecordPopulator;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;

/**
 * Controller responsible for taking action on notifications.
 *
 * @author Felix Hellman
 */
@RestController
public class NotificationController {

  private final CacheRecordPopulator populator;
  private final RegistryVerifier registryVerifier;

  /**
   * Constructor.
   * @param populator to notify
   * @param registryVerifier to verify notifications with
   */
  public NotificationController(
      final CacheRecordPopulator populator,
      final RegistryVerifier registryVerifier) {
    this.populator = populator;
    this.registryVerifier = registryVerifier;
  }

  /**
   * Handles notificaitons.
   * @param body notification
   * @throws InvalidRequestException if notification can not be verified.
   */
  @PostMapping("/registry/notify")
  public void notify(final String body) throws InvalidRequestException {
    try {
      this.registryVerifier.verifyNotification(body);
      this.populator.notifyPopulator();
    } catch (final RecordVerificationException e) {
      throw new InvalidRequestException("Could not verify notification");
    }
  }
}
