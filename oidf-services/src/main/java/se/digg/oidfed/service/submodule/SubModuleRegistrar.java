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
package se.digg.oidfed.service.submodule;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.modules.ResolverRegistrationEvent;
import se.digg.oidfed.service.resolver.ResolverFactory;

import java.util.List;

/**
 * Registrar for handling registrations/deregistrations of modules.
 *
 * @author Felix Hellman
 */
@Component
public class SubModuleRegistrar {
  private final InMemorySubModuleRegistry registry;
  private final ResolverFactory factory;

  /**
   * Constructor.
   * @param registry
   * @param factory
   */
  public SubModuleRegistrar(final InMemorySubModuleRegistry registry, final ResolverFactory factory) {
    this.registry = registry;
    this.factory = factory;
  }

  /**
   * Resolver registration.
   * @param event to handle
   */
  @EventListener
  public void handle(final ResolverRegistrationEvent event) {
    final Resolver resolver = this.factory.create(event.properties());
    this.registry.registerResolvers(List.of(resolver));
  }
}
