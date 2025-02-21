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
package se.digg.oidfed.service.modules;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.entity.EntitiesLoadedEvent;
import se.digg.oidfed.service.entity.ResolverInitEvent;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;

/**
 * Readystate component for loading resolvers
 *
 * @author Felix Hellman
 */
@Component
public class ResolverCacheInitializer extends ReadyStateComponent {

  private final ResolverCacheRegistry registry;
  private final ObservationRegistry observationRegistry;

  /**
   * @param registry of caches
   */
  public ResolverCacheInitializer(
      final ResolverCacheRegistry registry,
      final ObservationRegistry observationRegistry) {
    this.registry = registry;
    this.observationRegistry = observationRegistry;
  }

  @Override
  public String name() {
    return "resolver-cache-init";
  }

  @EventListener
  void handle(final ResolverInitEvent event) {
    this.registry.getAliases()
        .forEach(alias -> {
          final Observation resolveFederationObservation = Observation.start("Resolve federation %s".formatted(alias), this.observationRegistry);
          this.registry.loadTree(alias);
          resolveFederationObservation.stop();
        });
    this.markReady();
  }
}
