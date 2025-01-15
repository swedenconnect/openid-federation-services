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
package se.digg.oidfed.service.modules;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.resolver.ResolverConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * Responsible for converting properties to de/registration events.
 *
 * @author Felix Hellman
 */
@Component
public class ModuleSetup extends ReadyStateComponent {

  private final ResolverConfigurationProperties properties;
  private final KeyRegistry registry;
  private final ApplicationEventPublisher publisher;


  /**
   * Constructor.
   *
   * @param properties
   * @param registry
   * @param publisher
   */
  public ModuleSetup(
      final ResolverConfigurationProperties properties,
      final KeyRegistry registry,
      final ApplicationEventPublisher publisher) {
    this.properties = properties;
    this.registry = registry;
    this.publisher = publisher;
  }

  /**
   * Loads modules defined in properties on startup, only once.
   *
   * @param event to handle
   */
  @EventListener
  public void handle(final ApplicationStartedEvent event) {
    this.properties.getResolvers().stream()
        .map(resolver -> resolver.toResolverProperties(this.registry))
        .map(ResolverRegistrationEvent::new)
        .forEach(this.publisher::publishEvent);
  }

  /**
   * Handles {@link SubModulesReadEvent} containing information read from registry.
   *
   * @param event to handle
   * @return event for triggering next step in the load phase
   */
  @EventListener
  public ModuleSetupCompleteEvent handle(final SubModulesReadEvent event) {
    if (Objects.nonNull(event.modules())) {
      final ModuleResponse modules = event.modules();
      this.handleModules(modules);
    }
    markReady();
    return new ModuleSetupCompleteEvent();
  }

  private void handleModules(final ModuleResponse modules) {
    final List<ResolverModuleResponse> resolvers = modules.getResolvers();
    this.handleResolvers(resolvers);
  }

  private void handleResolvers(final List<ResolverModuleResponse> resolvers) {
    resolvers.stream()
        .filter(ResolverModuleResponse::getActive)
        .map(ResolverModuleResponse::toProperty)
        .map(ResolverRegistrationEvent::new)
        .forEach(this.publisher::publishEvent);

    resolvers.stream()
        .filter(resolver -> !resolver.getActive())
        .map(ResolverModuleResponse::getAlias)
        .map(ResolverDeregisterEvent::new)
        .forEach(this.publisher::publishEvent);
  }

  @Override
  public String name() {
    return "module-setup";
  }
}
