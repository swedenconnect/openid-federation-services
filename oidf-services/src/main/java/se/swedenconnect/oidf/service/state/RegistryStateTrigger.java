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
package se.swedenconnect.oidf.service.state;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled trigger for {@link RegistryStateManager}. Can be disabled via
 * {@code federation.service.scheduling.registry-trigger-enabled=false}.
 *
 * @author Felix Hellman
 */
public class RegistryStateTrigger {

  private final RegistryStateManager registryStateManager;

  /**
   * Constructor.
   *
   * @param registryStateManager the manager to trigger
   */
  public RegistryStateTrigger(final RegistryStateManager registryStateManager) {
    this.registryStateManager = registryStateManager;
  }

  /**
   * Trigger reload of registry state on a fixed schedule.
   */
  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public void reload() {
    this.registryStateManager.reload();
  }
}
