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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.service.health.ReadyStateComponent;
import se.swedenconnect.oidf.service.resolver.cache.CompositeTreeLoader;

/**
 * Readystate component for loading resolvers.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class ResolverStateManager extends ReadyStateComponent {

  private final CompositeTreeLoader treeLoader;
  private final ServiceLock redisServiceLock;
  private final ObservationRegistry registry;

  /**
   * Constructor.
   *
   * @param treeLoader
   * @param redisServiceLock
   * @param registry
   */
  public ResolverStateManager(
      final CompositeTreeLoader treeLoader,
      final ServiceLock redisServiceLock,
      final ObservationRegistry registry) {
    this.treeLoader = treeLoader;
    this.redisServiceLock = redisServiceLock;
    this.registry = registry;
  }

  @Override
  public String name() {
    return "resolver-state-manager";
  }

  /**
   * Trigger reload of this component.
   */
  public void reload() {
    log.debug("Reload resolver triggered from cron");
    if (this.ready()) {
      //No need to execute cron job during startup
      this.reloadResolvers();
    }
  }

  @EventListener
  void handle(final RegistryReadyEvent event) {
    try {
      log.debug("Triggering reload resolvers from ready event");
      this.reloadResolvers();
    } finally {
      log.debug("Resolver ready for traffic");
      this.markReady();
    }
  }

  @EventListener
  void handle(final RegistryLoadedEvent event) {
    if (this.ready()) {
      log.debug("Triggering reload resolvers from loaded event");
      this.reloadResolvers();
    }
  }

  private void reloadResolvers() {
    final Observation resolverReload = Observation.createNotStarted("resolver_reload", this.registry);
    resolverReload.observe(() -> {
      log.info("Starting resolver reload");
      if (this.redisServiceLock.acquireLock(this.name())) {
        log.info("Lock acquired");
        try {
          // --- Critical Section Start ---
          log.info("Start Tree load");
          this.treeLoader.loadTree();
          log.info("Tree loaded");
          // --- Critical Section End
        } finally {
          this.redisServiceLock.close(this.name());
          log.info("Lock closed");
        }
        log.info("Resolver reload finished.");
      }
    });
  }
}

