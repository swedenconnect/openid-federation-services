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
import se.digg.oidfed.service.modules.TrustAnchorRegistrationEvent;
import se.digg.oidfed.service.modules.TrustMarkIssuerRegistrationEvent;
import se.digg.oidfed.service.resolver.ResolverFactory;
import se.digg.oidfed.service.trustanchor.TrustAnchorFactory;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerFactory;
import se.digg.oidfed.trustanchor.TrustAnchor;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;

import java.util.List;

/**
 * Registrar for handling registrations/deregistrations of modules.
 *
 * @author Felix Hellman
 */
@Component
public class SubModuleRegistrar {
  private final InMemorySubModuleRegistry registry;
  private final ResolverFactory resolverFactory;
  private final TrustAnchorFactory trustAnchorFactory;
  private final TrustMarkIssuerFactory trustMarkIssuerFactory;


  /**
   * Constructor.
   *
   * @param registry
   * @param resolverFactory
   * @param trustAnchorFactory
   * @param trustMarkIssuerFactory
   */
  public SubModuleRegistrar(
      final InMemorySubModuleRegistry registry,
      final ResolverFactory resolverFactory,
      final TrustAnchorFactory trustAnchorFactory,
      final TrustMarkIssuerFactory trustMarkIssuerFactory) {
    this.registry = registry;
    this.resolverFactory = resolverFactory;
    this.trustAnchorFactory = trustAnchorFactory;
    this.trustMarkIssuerFactory = trustMarkIssuerFactory;
  }

  /**
   * Resolver registration.
   *
   * @param event to handle
   */
  @EventListener
  public void handle(final ResolverRegistrationEvent event) {
    final Resolver resolver = this.resolverFactory.create(event.properties());
    this.registry.registerResolvers(List.of(resolver));
  }

  /**
   * Trust-anchor registration.
   *
   * @param event to handle
   */
  @EventListener
  public void handle(final TrustAnchorRegistrationEvent event) {
    final TrustAnchor trustAnchor = this.trustAnchorFactory.create(event.properties());
    this.registry.registerTrustAnchor(List.of(trustAnchor));
  }

  /**
   * Trust mark issuer registration.
   *
   * @param event to handle
   */
  @EventListener
  public void handle(final TrustMarkIssuerRegistrationEvent event) {
    final TrustMarkIssuer trustMarkIssuer = this.trustMarkIssuerFactory.create(event.property());
    this.registry.registerTrustMarkIssuer(List.of(trustMarkIssuer));
  }
}
