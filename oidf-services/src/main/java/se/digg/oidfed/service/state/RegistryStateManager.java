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
package se.digg.oidfed.service.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.entity.integration.CacheRecordPopulator;
import se.digg.oidfed.common.entity.integration.registry.records.CompositeRecord;
import se.digg.oidfed.service.health.ReadyStateComponent;

@Slf4j
@Component
public class RegistryStateManager extends ReadyStateComponent {
  private final CacheRecordPopulator populator;
  private final FederationServiceState state;
  private final ServiceLock serviceLock;

  public RegistryStateManager(
      final CacheRecordPopulator populator,
      final FederationServiceState state,
      final ServiceLock serviceLock) {

    this.populator = populator;
    this.state = state;
    this.serviceLock = serviceLock;
  }

  /**
   * @param event to trigger from
   * @return event to trigger next step
   */
  @EventListener
  public Events.RegistryLoadedEvent init(final ApplicationStartedEvent event) {
    if (this.state.isStateMissing()) {
      //If no state is present we still need something for steps further down to compare towards.
      this.state.updateRegistryState("properties");
    }
    this.reloadFromRegistry();
    this.markReady();
    return new Events.RegistryLoadedEvent();
  }

  /**
   * Trigger reload of this component if needed.
   */
  @Scheduled(cron = "0 * * * *")
  public void reload() {
    if (this.ready()) {
      //No need to execute cron job during startup
      this.reloadFromRegistry();
    }
  }

  private void reloadFromRegistry() {
    if (this.populator.shouldRefresh()) {
      if (this.serviceLock.acquireLock(this.name())) {
        // --- Critical Section Start ---
        try {
          final CompositeRecord record = this.populator.reload();
          try {
            final String registrySha256 = StateHashFactory.hashState(record);
            this.state.updateRegistryState(registrySha256);
          } catch (final Exception e) {
            log.error("Failed to serialize state");
          }
        } catch (final Exception e) {
          log.error("Failed to load from registry", e);
        }
        this.serviceLock.close(this.name());
        // --- Critical Section End ---
      }
    }
  }

  @Override
  protected String name() {
    return "registry-state-manager";
  }
}
