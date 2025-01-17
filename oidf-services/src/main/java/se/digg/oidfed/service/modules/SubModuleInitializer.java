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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.health.ReadyStateComponent;

/**
 * Handles initialization of modules.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class SubModuleInitializer extends ReadyStateComponent {

  private final RestClientSubModuleIntegration integration;
  private final OpenIdFederationConfigurationProperties properties;

  /**
   * Constructor.
   *
   * @param integration
   * @param properties
   */
  public SubModuleInitializer(
      final RestClientSubModuleIntegration integration,
      final OpenIdFederationConfigurationProperties properties) {
    this.integration = integration;
    this.properties = properties;
  }

  @EventListener
  SubModulesReadEvent handle(final ApplicationStartedEvent event) {
    final OpenIdFederationConfigurationProperties.Registry.Integration integrationProperties = this.properties
        .getRegistry()
        .getIntegration();

    if (integrationProperties.shouldExecute(OpenIdFederationConfigurationProperties.Registry.Step.SUBMODULE)) {
      try {
        return new SubModulesReadEvent(this.integration.fetch(integrationProperties.getInstanceId()));
      } catch (final Exception e) {
        log.warn("Failed to communicate with registry, continuing ...", e);
        //throw new RuntimeException(e);
      }
    }
    markReady();
    return new SubModulesReadEvent(null);
  }

  @Override
  public String name() {
    return "module-init";
  }
}
