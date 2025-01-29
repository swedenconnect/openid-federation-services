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

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.resolver.cache.CacheRegistry;

/**
 * Readystate component for loading resolvers
 *
 * @author Felix Hellman
 */
@Component
public class ResolverInitializer extends ReadyStateComponent {

  private final CacheRegistry registry;

  /**
   * @param registry of caches
   */
  public ResolverInitializer(final CacheRegistry registry) {
    this.registry = registry;
  }

  @Override
  public String name() {
    return "resolver-init";
  }

  @EventListener
  void handle(final TrustMarkIssuerInitializedEvent event) {
    this.registry.getAliases()
        .forEach(this.registry::loadTree);
    this.markReady();
  }
}
