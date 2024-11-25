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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordIntegration;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.EntityRecordVerifier;
import se.digg.oidfed.common.keys.KeyRegistry;

/**
 * Initializer class for entity registry.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class EntityInitializer {

  private final EntityRouter router;
  private final EntityConfigurationProperties properties;
  private final KeyRegistry keyRegistry;
  private final EntityRecordRegistry registry;
  private final EntityRecordIntegration integration;
  private final EntityRecordVerifier verifier;


  /**
   * @param router to reload
   * @param properties to use
   * @param keyRegistry to use
   * @param registry to use
   * @param integration to use
   * @param verifier to use
   */
  public EntityInitializer(
      final EntityRouter router,
      final EntityConfigurationProperties properties,
      final KeyRegistry keyRegistry,
      final EntityRecordRegistry registry,
      final EntityRecordIntegration integration,
      final EntityRecordVerifier verifier) {

    this.router = router;
    this.properties = properties;
    this.keyRegistry = keyRegistry;
    this.registry = registry;
    this.integration = integration;
    this.verifier = verifier;
  }

  @EventListener
  void handle(final ApplicationStartedEvent event) {
    properties.getEntityRegistry()
        .stream().map(r -> r.toEntityRecord(keyRegistry))
        .toList().forEach(registry::addEntity);

    final EntityRecord issuerRecord = registry.getEntity("/").orElseThrow(() -> new IllegalArgumentException(
        "Could not find root entity in configuration"));
    final EntityID issuer = issuerRecord.getIssuer();
    try {
      final SignedJWT all = integration.getAll(issuer);
      verifier.verify(all).forEach(registry::addEntity);
    } catch (final Exception e) {
      log.error("failed to fetch entity records from registry", e);
    }
    router.reevaluteEndpoints();
  }
}
