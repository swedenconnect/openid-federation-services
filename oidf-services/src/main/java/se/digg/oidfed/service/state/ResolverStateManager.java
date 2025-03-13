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
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.resolver.cache.CompositeTreeLoader;

/**
 * Readystate component for loading resolvers
 *
 * @author Felix Hellman
 */
@Component
public class ResolverStateManager extends ReadyStateComponent {

  private final CompositeTreeLoader treeLoader;
  private final FederationServiceState state;
  private final ServiceLock redisServiceLock;
  private final OpenIdFederationConfigurationProperties properties;

  /**
   * Constructor.
   *
   * @param treeLoader
   * @param state
   * @param redisServiceLock
   */
  public ResolverStateManager(final CompositeTreeLoader treeLoader,
                              final FederationServiceState state,
                              final ServiceLock redisServiceLock) {
    this.treeLoader = treeLoader;
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
  void handle(final RegistryLoadedEvent event) {
    this.reloadResolvers();
    this.markReady();
  }

  private void reloadResolvers() {
    if (this.redisServiceLock.acquireLock(this.name())) {
      final String registryState = this.state.getRegistryState();
      //If registry integration is disabled, refresh resolver anyway
      final boolean reEvaluateCondition = this.state.resolverNeedsReevaulation(registryState)
          || !this.properties.getRegistry().getIntegration().getEnabled();
      if (reEvaluateCondition) {
        // --- Critical Section Start ---
        this.treeLoader.loadTree();
        this.state.updateResolverState(registryState);
        this.redisServiceLock.close(this.name());
        // --- Critical Section End
      }
    }
  }
}
