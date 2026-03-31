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
package se.swedenconnect.oidf.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntityLookup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Router responsible for matching any entity configuration endpoints.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityRouter implements Router {

  private final EntityConfigurationFactory factory;
  private final RouteFactory routeFactory;
  private final ScrapedEntityLookup lookup;

  /**
   * Constructor.
   *
   * @param factory      for creating entity configurations
   * @param routeFactory for creating routes
   * @param lookup       lookup for scraped entities
   */
  public EntityRouter(
      final EntityConfigurationFactory factory,
      final RouteFactory routeFactory,
      final ScrapedEntityLookup lookup) {
    this.factory = factory;
    this.routeFactory = routeFactory;
    this.lookup = lookup;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
      return source.getAllEntities().stream()
          .map(entity -> this.getRouteForEntity(entity))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    }, request -> {
      final EntityRecord entityRecord = source.getAllEntities().stream()
          .filter(entity -> this.getRouteForEntity(entity)
              .test(request))
          .findFirst()
          .get();
      return this.handleCacheControl(request, entityRecord.getEntityIdentifier())
          .orElseGet(() -> ServerResponse.ok()
              .body(this.factory.createEntityConfiguration(entityRecord).getSignedStatement()
                  .serialize()));
    });
  }

  private Optional<ServerResponse> handleCacheControl(final ServerRequest request, final EntityID entityId) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.lookup.findByEntityId(entityId.getValue(), a -> true)
          .filter(entity -> Objects.nonNull(entity.getEntityStatement()))
          .map(entity -> ServerResponse.ok()
              .body(entity.getEntityStatement().getSignedStatement().serialize()));
    }
    return Optional.empty();
  }

  private RequestPredicate getRouteForEntity(final EntityRecord entity) {
    final Optional<RequestPredicate> alternateRoute = Optional.ofNullable(entity.getEcLocation())
        .map(this.routeFactory::createAlternateRoute);

    final RequestPredicate defaultRoute = this.routeFactory
        .createRoute(entity.getEntityIdentifier(), "/.well-known/openid-federation",
            Objects.isNull(entity.getEcLocation()));

    return alternateRoute
        .map(requestPredicate -> requestPredicate.or(defaultRoute))
        .orElse(defaultRoute);
  }
}
