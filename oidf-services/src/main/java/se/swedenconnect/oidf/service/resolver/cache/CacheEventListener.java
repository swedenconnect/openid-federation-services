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
package se.swedenconnect.oidf.service.resolver.cache;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for cache operations.
 *
 * @author Felix Hellman
 */
@Component
public class CacheEventListener {

  private final ResolverCacheRegistry registry;

  /**
   * Constructor.
   * @param registry to perform actions upon
   */
  public CacheEventListener(final ResolverCacheRegistry registry) {
    this.registry = registry;
  }

  @EventListener
  void handle(final TreeUpdatedEvent event) {
    this.registry.updateVersion(event.entityId());
  }
}
