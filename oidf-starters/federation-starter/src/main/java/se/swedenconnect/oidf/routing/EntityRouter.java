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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationFactory;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.EntityConfigurationCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.routing.ModuleRouter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Router responsible for matching any entity configuration endpoints.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityRouter implements Router, ModuleRouter {

  private final EntityConfigurationFactory factory;
  private final RouteFactory routeFactory;
  private final CacheSnapshotVersionLookup lookup;
  private final EntityConfigurationCache entityConfigurationCache;
  private final ObservationRegistry observationRegistry;

  /**
   * Constructor.
   *
   * @param factory                  for creating entity configurations
   * @param routeFactory             for creating routes
   * @param lookup                   lookup for scraped entities
   * @param entityConfigurationCache cache for entity configuration responses
   * @param observationRegistry      for recording observations
   */
  public EntityRouter(
      final EntityConfigurationFactory factory,
      final RouteFactory routeFactory,
      final CacheSnapshotVersionLookup lookup,
      final EntityConfigurationCache entityConfigurationCache,
      final ObservationRegistry observationRegistry) {
    this.factory = factory;
    this.routeFactory = routeFactory;
    this.lookup = lookup;
    this.entityConfigurationCache = entityConfigurationCache;
    this.observationRegistry = observationRegistry;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
      return source.getAllEntities().stream()
          .map(entity -> this.getRouteForEntity(entity))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    }, request -> {
      final Long snapshot = this.lookup.getLatestSnapshotVersion();
      final EntityRecord entityRecord = source.getAllEntities().stream()
          .filter(entity -> this.getRouteForEntity(entity)
              .test(request))
          .findFirst()
          .get();
      final Observation observation = this.observationRegistry.getCurrentObservation();
      final String endpoint = request.requestPath().value();
      if (observation != null) {
        observation.lowCardinalityKeyValue("endpoint", endpoint);
      }
      return this.handleCacheControl(request, entityRecord.getEntityIdentifier(), snapshot)
          .map(response -> {
            if (observation != null) {
              observation.lowCardinalityKeyValue("cached", "true");
            }
            return response;
          })
          .orElseGet(() -> {
            if (observation != null) {
              observation.lowCardinalityKeyValue("cached", "false");
            }
            final EntityStatement entityConfiguration = this.factory.createEntityConfiguration(entityRecord);
            this.entityConfigurationCache.put(snapshot,entityRecord.getEntityIdentifier().getValue(),
                entityConfiguration.getSignedStatement().serialize());
            return ServerResponse.ok().body(entityConfiguration.getSignedStatement().serialize());
          });
    });
  }

  @Override
  public CachedResponse handleRequest(final ServerRequest request, final EntityRecord entity) {
    final Observation observation = Observation.createNotStarted("oidf.entity.configuration", this.observationRegistry)
        .lowCardinalityKeyValue("endpoint", "/.well-known/openid-federation")
        .start();
    try {
      final EntityStatement entityConfiguration = this.factory.createEntityConfiguration(entity);
      final String serialized = entityConfiguration.getSignedStatement().serialize();
      return new CachedResponse(serialized, "application/entity-statement+jwt", 200);
    } catch (final Exception e) {
      observation.error(e);
      throw e;
    } finally {
      observation.stop();
    }
  }

  @Override
  public boolean willHandleRequest(final ServerRequest request, final EntityRecord entity) {
    return entity
        .getEntityConfigurationEndpoints()
        .stream()
        .anyMatch(endpoint -> request.uri().toASCIIString().equalsIgnoreCase(endpoint));
  }

  private Optional<ServerResponse> handleCacheControl(final ServerRequest request,
                                                      final EntityID entityId,
                                                      final long snapshot) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      final Optional<String> cached = this.entityConfigurationCache.get(snapshot, entityId.getValue());
      if (cached.isPresent()) {
        return cached.map(response -> ServerResponse.ok().body(response));
      }
      log.info("Cache miss! {}", entityId);
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
