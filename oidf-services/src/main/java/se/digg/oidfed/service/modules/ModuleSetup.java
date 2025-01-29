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

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.trustanchor.TrustAnchorModuleProperties;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerModuleProperties;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Responsible for converting properties to de/registration events.
 *
 * @author Felix Hellman
 */
@Component
public class ModuleSetup extends ReadyStateComponent {

  private final OpenIdFederationConfigurationProperties properties;
  private final KeyRegistry registry;
  private final ApplicationEventPublisher publisher;


  /**
   * Constructor.
   * @param properties
   * @param registry
   * @param publisher
   */
  public ModuleSetup(
      final OpenIdFederationConfigurationProperties properties,
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
    Optional.ofNullable(this.properties.getModules())
            .ifPresent(modules -> this.configureFromProperties());
  }

  private void configureFromProperties() {
    Optional.ofNullable(this.properties.getModules().getResolvers())
        .ifPresent(resolvers -> resolvers.stream()
            .map(resolver -> resolver.toResolverProperties(this.registry))
            .map(ResolverRegistrationEvent::new)
            .forEach(this.publisher::publishEvent));


    Optional.ofNullable(this.properties.getModules().getTrustAnchors())
        .ifPresent(ta -> ta.stream()
            .map(TrustAnchorModuleProperties.TrustAnchorSubModuleProperties::toTrustAnchorProperties)
            .map(TrustAnchorRegistrationEvent::new)
            .forEach(this.publisher::publishEvent));

    Optional.ofNullable(this.properties.getModules().getTrustMarkIssuers())
        .ifPresent(tmi -> tmi.stream()
            .map(TrustMarkIssuerModuleProperties.TrustMarkIssuerSubModuleProperty::toProperties)
            .map(TrustMarkIssuerRegistrationEvent::new)
            .forEach(this.publisher::publishEvent));
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
    this.handleResolvers(modules.getResolvers());
    this.handleTrustAnchors(modules.getTrustAnchors());
  }

  private void handleTrustAnchors(final List<TrustAnchorModuleResponse> trustAnchors) {
    trustAnchors.stream()
        .filter(TrustAnchorModuleResponse::getActive)
        .map(TrustAnchorModuleResponse::toProperties)
        .map(TrustAnchorRegistrationEvent::new)
        .forEach(this.publisher::publishEvent);

    trustAnchors.stream()
        .filter(anchor -> !anchor.getActive())
        .map(TrustAnchorModuleResponse::getAlias)
        .map(TrustAnchorDeregisterEvent::new)
        .forEach(this.publisher::publishEvent);
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
