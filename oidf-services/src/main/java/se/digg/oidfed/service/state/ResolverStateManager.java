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
import se.digg.oidfed.service.resolver.cache.CompositeTreeLoader;

import java.util.concurrent.TimeUnit;

/**
 * Readystate component for loading resolvers.
 *
 * @author Felix Hellman
 */
@Component
public class ResolverStateManager extends ReadyStateComponent {

  private final CompositeTreeLoader treeLoader;
  private final ServiceLock redisServiceLock;

  /**
   * Constructor.
   *
   * @param treeLoader
   * @param redisServiceLock
   */
  public ResolverStateManager(
      final CompositeTreeLoader treeLoader,
      final ServiceLock redisServiceLock) {
    this.treeLoader = treeLoader;
    this.redisServiceLock = redisServiceLock;
  }

  @Override
  public String name() {
    return "resolver-state-manager";
  }

  /**
   * Trigger reload of this component if needed.
   */
  @Scheduled(fixedRate = 60, timeUnit = TimeUnit.MINUTES)
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
      // --- Critical Section Start ---
      this.treeLoader.loadTree();
      this.redisServiceLock.close(this.name());
      // --- Critical Section End
    }
  }
}

