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
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.modules.ModuleSetupCompleteEvent;
import se.digg.oidfed.service.submodule.InMemorySubModuleRegistry;

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
  private final EntityConfigurationProperties properties;
  private final PolicyConfigurationProperties policyProperties;
  private final KeyRegistry keyRegistry;
  private final EntityRecordRegistry registry;
  private final RecordRegistrySource source;
  private final InMemorySubModuleRegistry subModuleRegistry;

  /**
   * Constructor.
   *
   * @param router            to update
   * @param properties        to read from
   * @param policyProperties  to read from
   * @param keyRegistry       to read keys from
   * @param registry          to add records to
   * @param source            to get records from
   * @param subModuleRegistry to get modules from
   */
  public EntityInitializer(
      final EntityRouter router,
      final EntityConfigurationProperties properties,
      final PolicyConfigurationProperties policyProperties,
      final KeyRegistry keyRegistry,
      final EntityRecordRegistry registry,
      final RecordRegistrySource source,
      final InMemorySubModuleRegistry subModuleRegistry) {
    this.router = router;
    this.properties = properties;
    this.policyProperties = policyProperties;
    this.keyRegistry = keyRegistry;
    this.registry = registry;
    this.source = source;
    this.subModuleRegistry = subModuleRegistry;
  }

  /**
   * @param event to handle
   * @return event to notify the system that entities has been loaded
   */
  @EventListener
  public EntitiesLoadedEvent handle(final ModuleSetupCompleteEvent event) {
    this.properties.getEntityRegistry()
        .stream().map(r -> r.toEntityRecord(this.keyRegistry))
        .toList().forEach(this.registry::addEntity);

    Optional.ofNullable(this.policyProperties.getPolicies()).ifPresent(
        pp -> pp.stream()
            .map(PolicyConfigurationProperties.PolicyRecordProperty::toRecord)
            .forEach(this.source::addPolicy)
    );

    if (!this.properties.isSkipRegistryInit()) {
      this.loadFromRegistry();
    }
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

    final List<EntityID> issuers = new ArrayList<>();
    //issuers.addAll(this.subModuleRegistry.getAllEntityIds());
    issuers.addAll(this.properties.getIssuers().stream().map(EntityID::new).toList());

    issuers.forEach(this::loadIssuerFromRegistry);
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
