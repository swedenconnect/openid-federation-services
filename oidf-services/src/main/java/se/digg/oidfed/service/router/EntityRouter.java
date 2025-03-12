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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.EntityConfigurationFactory;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;

/**
 * Router respoinsble for matching any entity configuration endpoints.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class EntityRouter implements Router {

  private final EntityConfigurationFactory factory;
  private final RouteFactory routeFactory;

  /**
   * Constructor.
   * @param factory for creating entity configurations
   * @param routeFactory for creating routes.
   */
  public EntityRouter(final EntityConfigurationFactory factory, final RouteFactory routeFactory) {
    this.factory = factory;
    this.routeFactory = routeFactory;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
      return source.getAllEntities().stream()
          .map(entity -> this.routeFactory.createRoute(entity.getSubject(), "/.well-known/openid-federation"))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    }, request -> {
      final EntityRecord entityRecord = source.getAllEntities().stream()
          .filter(entity -> this.routeFactory.createRoute(entity.getSubject(), "/.well-known/openid-federation")
              .test(request))
          .findFirst()
          .get();
      return ServerResponse.ok()
          .body(this.factory.createEntityConfiguration(entityRecord).getSignedStatement()
              .serialize());
    });
  }
}
