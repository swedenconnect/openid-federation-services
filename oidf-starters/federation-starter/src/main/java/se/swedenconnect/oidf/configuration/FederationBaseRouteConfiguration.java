/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.configuration;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityConfigurationCache;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.routing.EntityRouter;
import se.swedenconnect.oidf.routing.JWKSRouter;
import se.swedenconnect.oidf.routing.RouteFactory;

/**
 * Configuration for adding default routers.
 *
 * @author Felix Hellman
 */
@Configuration
public class FederationBaseRouteConfiguration {
  @Bean
  EntityRouter entityRouter(final EntityConfigurationFactory entityConfigurationFactory, final RouteFactory factory,
      final CacheSnapshotVersionLookup lookup, final EntityConfigurationCache entityConfigurationCache,
      final ObservationRegistry observationRegistry) {
    return new EntityRouter(entityConfigurationFactory, factory, lookup, entityConfigurationCache, observationRegistry);
  }

  @Bean
  JWKSRouter jwksRouter(final KeyRegistry registry) {
    return new JWKSRouter(registry);
  }
}
