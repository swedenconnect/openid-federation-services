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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;

/**
 * Readystate component for loading resolvers
 *
 * @author Felix Hellman
 */
@Component
public class ResolverStateManager extends ReadyStateComponent {

  private final ResolverCacheRegistry registry;
  private final ObservationRegistry observationRegistry;
  private final FederationServiceState state;
  private final ServiceLock redisServiceLock;

  public ResolverStateManager(final ResolverCacheRegistry registry,
                              final ObservationRegistry observationRegistry,
                              final FederationServiceState state,
                              final ServiceLock redisServiceLock) {
    this.registry = registry;
    this.observationRegistry = observationRegistry;
    this.state = state;
    this.redisServiceLock = redisServiceLock;
  }

  @Override
  public String name() {
    return "resolver-state-manager";
  }

  /**
   * Trigger reload of this component if needed.
   */
  @Scheduled(cron = "0 * * * * *")
  public void reload() {
    if (this.ready()) {
      //No need to execute cron job during startup
      this.reloadResolvers();
    }
  }

  @EventListener
  void handle(final Events.RouterLoadedEvent event) {
    this.reloadResolvers();
    this.markReady();
  }

  private void reloadResolvers() {
    final String routerState = this.state.getRouterState();
    if (this.state.resolverNeedsReevaulation()) {
      if (this.redisServiceLock.acquireLock(this.name())) {
        // --- Critical Section Start ---
        this.registry.getAliases()
            .forEach(alias -> {
              final Observation resolveFederationObservation =
                  Observation.start(
                      "Resolve federation %s".formatted(alias),
                      this.observationRegistry
                  );
              this.registry.loadTree(alias);
              resolveFederationObservation.stop();
            });
        this.state.updateResolverState(routerState);
        this.redisServiceLock.close(this.name());
        // --- Critical Section End
      }
    }
  }
}
