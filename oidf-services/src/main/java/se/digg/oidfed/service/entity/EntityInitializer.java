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
package se.digg.oidfed.service.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.keys.FederationKeys;
import se.digg.oidfed.service.modules.ModuleSetupCompleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Initializer class for entity registry.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class EntityInitializer extends ReadyStateComponent {

  private final EntityRouter router;
  private final OpenIdFederationConfigurationProperties properties;
  private final KeyRegistry keyRegistry;
  private final FederationKeys keys;
  private final EntityRecordRegistry registry;
  private final RecordRegistrySource source;

  /**
   * Constructor
   * @param router
   * @param properties
   * @param keyRegistry
   * @param keys
   * @param registry
   * @param source
   */
  public EntityInitializer(
      final EntityRouter router,
      final OpenIdFederationConfigurationProperties properties,
      final KeyRegistry keyRegistry,
      final FederationKeys keys,
      final EntityRecordRegistry registry,
      final RecordRegistrySource source) {
    this.router = router;
    this.properties = properties;
    this.keyRegistry = keyRegistry;
    this.keys = keys;
    this.registry = registry;
    this.source = source;
  }

  /**
   * @param event to handle
   * @return event to notify the system that entities has been loaded
   */
  @EventListener
  public EntitiesLoadedEvent handle(final ModuleSetupCompleteEvent event) {
    this.properties.getEntities()
        .stream().map(r -> r.toEntityRecord(this.keyRegistry, this.keys))
        .toList().forEach(this.registry::addEntity);

    Optional.ofNullable(this.properties.getPolicies()).ifPresent(
        pp -> pp.stream()
            .map(PolicyConfigurationProperties.PolicyRecordProperty::toRecord)
            .forEach(this.source::addPolicy)
    );

    this.router.reevaluteEndpoints();
    markReady();
    return new EntitiesLoadedEvent();
  }

  /**
   * Event listener for reloading all entities from the register.
   *
   * @param event to trigger on
   * @return event to notify the system that entities has been loaded
   */
  @EventListener
  public EntitiesLoadedEvent handleReload(final EntityReloadEvent event) {
    log.debug("Handling entity reload event");
    this.loadFromRegistry();
    this.router.reevaluteEndpoints();
    return new EntitiesLoadedEvent();
  }

  private void loadFromRegistry() {
    final List<OpenIdFederationConfigurationProperties.Registry.Step> skipInit = this.properties
        .getRegistry()
        .getIntegration()
        .getSkipInit();

    if (!skipInit.contains(OpenIdFederationConfigurationProperties.Registry.Step.ENTITY)) {
      final List<EntityID> issuers = new ArrayList<>(this.properties.getModules()
          .getIssuers()
          .stream()
          .map(EntityID::new)
          .toList()
      );
      issuers.forEach(this::loadIssuerFromRegistry);
    }
  }

  private void loadIssuerFromRegistry(final EntityID issuer) {
    try {
      this.source.getEntityRecords(issuer.getValue())
          .forEach(this.registry::addEntity);
    } catch (final Exception e) {
      log.error("failed to fetch entity records from registry", e);
    }
  }

  @Override
  public String name() {
    return "entity-init";
  }
}
