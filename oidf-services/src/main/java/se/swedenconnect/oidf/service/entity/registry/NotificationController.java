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
package se.swedenconnect.oidf.service.entity.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.common.entity.entity.RecordVerificationException;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryVerifier;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.service.state.RegistryStateManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller responsible for taking action on notifications.
 *
 * @author Felix Hellman
 */
@Slf4j
@RestController
public class NotificationController {

  private final RegistryVerifier registryVerifier;
  private final RegistryStateManager registryStateManager;
  private final AtomicBoolean hasPending = new AtomicBoolean(false);

  /**
   * Constructor.
   * @param registryVerifier to verify notifications with
   * @param registryStateManager to trigger forced reload
   */
  public NotificationController(
      final RegistryVerifier registryVerifier,
      final RegistryStateManager registryStateManager) {
    this.registryVerifier = registryVerifier;
    this.registryStateManager = registryStateManager;
  }

  /**
   * Handles notificaitons.
   * @param body notification
   * @throws InvalidRequestException if notification can not be verified.
   */
  @PostMapping(value = "/registry/notify")
  public void notify(@RequestBody final String body) throws InvalidRequestException {
    try {
      this.registryVerifier.verifyNotification(body);
      log.debug("Notification verified, triggering forced federation reload");
      if (this.hasPending.compareAndSet(false, true)) {
        Thread.ofVirtual().start(() -> {
          try {
            Thread.sleep(1000);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
          try {
            this.registryStateManager.forceReload();
          } finally {
            this.hasPending.set(false);
          }
        });
      } else {
        log.debug("Reload already pending, notification coalesced");
      }
    } catch (final RecordVerificationException e) {
      throw new InvalidRequestException("Could not verify notification");
    }
  }

}
