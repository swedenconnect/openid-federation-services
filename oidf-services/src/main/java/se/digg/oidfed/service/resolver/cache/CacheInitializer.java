/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.service.resolver.cache;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.entity.EntitiesInitializedEvent;
import se.digg.oidfed.service.resolver.OnResolverModuleActive;

/**
 * Initializes state and makes the service ready for traffic.
 *
 * @author Felix Hellman
 */
@Component
@OnResolverModuleActive
public class CacheInitializer {

  private final CacheRegistry registry;

  /**
   * Constructor.
   * @param registry to use
   */
  public CacheInitializer(final CacheRegistry registry) {
    this.registry = registry;
  }

  @EventListener
  void handle(final EntitiesInitializedEvent event) {
    this.registry.getAliases().forEach(this.registry::loadTree);
  }
}
