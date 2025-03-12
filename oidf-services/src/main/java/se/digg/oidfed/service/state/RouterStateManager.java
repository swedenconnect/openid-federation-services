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

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.router.MainRouter;

@Component
public class RouterStateManager extends ReadyStateComponent {
  private final MainRouter router;
  private final FederationServiceState state;

  public RouterStateManager(
      final MainRouter router,
      final FederationServiceState state) {
    this.router = router;
    this.state = state;
  }

  /**
   * Trigger reload of this component if needed.
   */
  @Scheduled(cron = "0 * * * * *")
  public void reload() {
    if (this.ready()) {
      //No need to execute cron job during startup
      this.reloadRouter();
    }
  }

  @EventListener
  Events.RouterLoadedEvent handle(final Events.RegistryLoadedEvent event) {
    this.reloadRouter();
    this.markReady();
    return new Events.RouterLoadedEvent();
  }

  private void reloadRouter() {
    final String registryState = this.state.getRegistryState();
    if (!this.state.isRouterStateCurrent(registryState)) {
      //Router needs to reload
      //This operation needs to be done on each node
      this.router.reEvaluateEndpoints();
      this.state.updateRouterState(registryState);
    }
  }

  @Override
  protected String name() {
    return "router-state-manager";
  }
}
