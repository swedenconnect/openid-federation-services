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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.service.health.ReadyStateComponent;
import se.swedenconnect.oidf.service.resolver.cache.CompositeTreeLoader;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final AtomicBoolean initialLoadTriggered = new AtomicBoolean(false);

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
   * Trigger reload of this component. Also retries the initial load when it failed, so that a resolver that
   * could not load its trees at startup becomes ready as soon as the federation can be read again.
   */
  public void reload() {
    log.debug("Reload resolver triggered from cron");
    if (this.initialLoadTriggered.get()) {
      //No need to execute cron job before the initial load has been triggered
      this.loadResolvers();
    }
  }

  @EventListener
  void handle(final RegistryReadyEvent event) {
    log.debug("Triggering reload resolvers from ready event");
    this.initialLoadTriggered.set(true);
    this.loadResolvers();
  }

  @EventListener
  void handle(final RegistryLoadedEvent event) {
    if (this.initialLoadTriggered.get()) {
      log.debug("Triggering reload resolvers from loaded event");
      this.loadResolvers();
    }
  }

  /**
   * Loads the resolver trees and keeps the ready state in sync with the outcome. A failed load is logged
   * instead of being thrown: at startup it would otherwise shut the service down, and the trees of the
   * previous load are still served while the next load is attempted.
   */
  private void loadResolvers() {
    try {
      this.reloadResolvers();
      if (!this.ready()) {
        log.info("Resolver ready for traffic");
        this.markReady();
      }
    } catch (final Exception e) {
      if (this.ready()) {
        log.error("Failed to reload resolvers, keeping the trees of the previous load", e);
      } else {
        log.error("Failed to load resolvers, resolver is not ready for traffic", e);
      }
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
          final Instant before = Instant.now();
          this.treeLoader.loadTree();
          log.info("Tree loaded in {}", Duration.between(before, Instant.now()));
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

