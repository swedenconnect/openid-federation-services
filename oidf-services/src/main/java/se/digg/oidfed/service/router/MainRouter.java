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
package se.digg.oidfed.service.router;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router for serving federation content.
 *
 * @author Felix Hellman
 */
@Configuration
@Slf4j
public class MainRouter {

  private final CompositeRecordSource source;
  private final List<Router> routers;
  private final ObservationRegistry registry;

  /**
   * Constructor.
   * @param source
   * @param routers
   * @param registry
   */
  public MainRouter(
      final CompositeRecordSource source,
      final List<Router> routers,
      final ObservationRegistry registry) {

    this.source = source;
    this.routers = routers;
    this.registry = registry;
  }

  @Bean
  RouterFunction<ServerResponse> federationEndpointRouter() {
    final RouterFunctions.Builder route = route();
    this.routers.forEach(router -> router.evaluateEndpoints(this.source, route));
    route.filter((request, next) -> {
      final Observation observation = Observation.createNotStarted("router_request", this.registry)
          .lowCardinalityKeyValue("uri", request.requestPath().value())
          .lowCardinalityKeyValue("method", request.method().name())
          .lowCardinalityKeyValue("exception", "none");
      return Objects.requireNonNull(observation.observe(() -> {
        try {
          return next.handle(request);
        } catch (final Exception e) {
          observation.lowCardinalityKeyValue("exception", e.getClass().getName());
          throw new RuntimeException(e);
        }
      }));
    });
    return route.build();
  }
}
