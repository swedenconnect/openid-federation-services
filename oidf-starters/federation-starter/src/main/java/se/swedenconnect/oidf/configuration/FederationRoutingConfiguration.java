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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.routing.Router;

import java.util.List;
import java.util.Objects;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router for serving federation content.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
public class FederationRoutingConfiguration {

  private final CompositeRecordSource source;
  private final List<Router> routers;
  private final ObservationRegistry registry;

  /**
   * Constructor.
   * @param source
   * @param routers
   * @param registry
   * @param virtualEntityRoutingEnabled when {@code true}, only {@link FederationBaseRouter} is used;
   *                                    old per-module routes are disabled
   */
  public FederationRoutingConfiguration(
      final CompositeRecordSource source,
      final List<Router> routers,
      final ObservationRegistry registry,
      @Value("${federation.routing.virtual-entity-routing.enabled:false}")
      final boolean virtualEntityRoutingEnabled) {

    this.source = source;
    this.routers = virtualEntityRoutingEnabled
        ? routers.stream().filter(a -> a instanceof FederationBaseRouter).toList()
        : routers;
    this.registry = registry;
  }

  @Bean
  RouterFunction<ServerResponse> federationEndpointRouter() {
    final RouterFunctions.Builder route = route();
    this.routers.stream()
        .sorted((a, b) -> a instanceof FederationBaseRouter ? -1 : b instanceof FederationBaseRouter ? 1 : 0)
        .forEach(router -> router.evaluateEndpoints(this.source, route));
    route.filter((request, next) -> {
        try {
          return next.handle(request);
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
    });
    return route.build();
  }
}
