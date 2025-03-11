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
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.EntityConfigurationFactory;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;

@Slf4j
@Component
public class EntityRouter implements Router {

  private final EntityConfigurationFactory factory;
  private final RouteFactory routeFactory;

  public EntityRouter(final EntityConfigurationFactory factory, final RouteFactory routeFactory) {
    this.factory = factory;
    this.routeFactory = routeFactory;
  }

  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    source.getAllEntities().stream()
        .filter(EntityRecord::isHosted)
        .forEach(entity -> {
          route.GET(
              this.routeFactory.createRoute(entity.getSubject(),
                  "/.well-known/openid-federation") ,
              request -> ServerResponse.ok().body(this.factory.createEntityConfiguration(entity).getSignedStatement().serialize()));
        });
  }
}
