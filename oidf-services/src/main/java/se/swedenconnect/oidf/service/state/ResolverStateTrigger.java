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
package se.swedenconnect.oidf.service.state;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled trigger for {@link ResolverStateManager}. Can be disabled via
 * {@code federation.service.scheduling.resolver-trigger-enabled=false}.
 * The reload rate can be configured via
 * {@code federation.service.scheduling.resolver-reload-rate} (ISO 8601 duration, default PT60M).
 *
 * @author Felix Hellman
 */
public class ResolverStateTrigger {

  private final ResolverStateManager resolverStateManager;

  /**
   * Constructor.
   *
   * @param resolverStateManager the manager to trigger
   */
  public ResolverStateTrigger(final ResolverStateManager resolverStateManager) {
    this.resolverStateManager = resolverStateManager;
  }

  /**
   * Trigger reload of resolver state on a fixed schedule.
   */
  @Scheduled(fixedRateString = "${federation.service.scheduling.resolver-reload-rate:PT60M}")
  public void reload() {
    this.resolverStateManager.reload();
  }
}
