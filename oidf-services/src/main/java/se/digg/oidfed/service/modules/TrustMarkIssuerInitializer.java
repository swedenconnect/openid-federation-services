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

  private final TrustMarkIssuerModuleProperties properties;
  private final TrustMarkSubjectRepository repository;
  private final RestClientTrustMarkSubjectRecordIntegration integration;

  /**
   * Constructor.
   * @param properties
   * @param repository
   * @param integration
   */
  public TrustMarkIssuerInitializer(
      final TrustMarkIssuerModuleProperties properties,
      final TrustMarkSubjectRepository repository,
      final RestClientTrustMarkSubjectRecordIntegration integration) {
    this.properties = properties;
    this.repository = repository;
    this.integration = integration;
  }

  /**
   * Trigger setup on module setup completed.
   * @param event to handle
   */
  @EventListener
  public void handle(final ModuleSetupCompleteEvent event) {
    this.properties.getTrustMarkIssuers().stream()
        .flatMap(tmi -> tmi.trustMarks().stream())
        .forEach(tm -> tm.subjects().forEach(sub -> this.repository.register(tm.trustMarkId(), sub.toSubject())));

    this.properties.getTrustMarkIssuers()
        .forEach(tmi -> tmi.trustMarks()
            .forEach(tm -> this.integration.loadSubject(tmi.entityIdentifier(), tm.trustMarkId(), Optional.empty())
                .forEach(subject -> this.repository.register(tm.trustMarkId(), subject))
            )
        );

    markReady();
  }

  @Override
  protected String name() {
    return "trust-mark-issuer-init";
  }
}
