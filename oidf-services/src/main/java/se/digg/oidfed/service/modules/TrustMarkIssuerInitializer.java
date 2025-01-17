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

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.health.ReadyStateComponent;
import se.digg.oidfed.service.trustmarkissuer.RestClientTrustMarkSubjectRecordIntegration;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerModuleProperties;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRepository;

import java.util.Optional;


/**
 * Initializer for {@link se.digg.oidfed.trustmarkissuer.TrustMarkIssuer}
 *
 * @author Felix Hellman
 */
@Component
public class TrustMarkIssuerInitializer extends ReadyStateComponent {

  private final OpenIdFederationConfigurationProperties properties;
  private final TrustMarkSubjectRepository repository;
  private final RestClientTrustMarkSubjectRecordIntegration integration;

  /**
   * Constructor.
   * @param properties
   * @param repository
   * @param integration
   */
  public TrustMarkIssuerInitializer(
      final OpenIdFederationConfigurationProperties properties,
      final TrustMarkSubjectRepository repository,
      final RestClientTrustMarkSubjectRecordIntegration integration) {
    this.properties = properties;
    this.repository = repository;
    this.integration = integration;
  }

  /**
   * Trigger setup on module setup completed.
   * @param event to handle
   * @return event to signal that trust mark issuers have been loaded
   */
  @EventListener
  public TrustMarkIssuerInitializedEvent handle(final ModuleSetupCompleteEvent event) {
    this.properties.getModules().getTrustMarkIssuers().stream()
        .flatMap(tmi -> tmi.trustMarks().stream())
        .forEach(tm -> tm.subjects().forEach(sub -> {
          this.repository.register(tm.trustMarkId(), sub.toSubject());
        }));
    final OpenIdFederationConfigurationProperties.Registry.Integration integrationProperties = this.properties
        .getRegistry()
        .getIntegration();

    if (integrationProperties.shouldExecute(OpenIdFederationConfigurationProperties.Registry.Step.TRUST_MARK_SUBJECT)) {
      this.properties.getModules().getTrustMarkIssuers()
          .forEach(tmi -> tmi.trustMarks()
              .forEach(tm -> this.integration.loadSubject(tmi.entityIdentifier(), tm.trustMarkId(), Optional.empty())
                  .forEach(subject -> this.repository.register(tm.trustMarkId(), subject))
              )
          );
    }
    markReady();
    return new TrustMarkIssuerInitializedEvent();
  }

  @Override
  protected String name() {
    return "trust-mark-issuer-init";
  }
}
