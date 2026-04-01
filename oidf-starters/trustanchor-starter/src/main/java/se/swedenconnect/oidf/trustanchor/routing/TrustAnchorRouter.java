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
package se.swedenconnect.oidf.trustanchor.routing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.SubordinateFetchCache;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.routing.RequireParameters;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.Router;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;
import se.swedenconnect.oidf.trustanchor.TrustAnchor;
import se.swedenconnect.oidf.trustanchor.TrustAnchorFactory;

import java.util.List;
import java.util.Optional;

/**
 * Responsible for matching trust anchor requests for any configured module.
 *
 * @author Felix Hellman
 */
public class TrustAnchorRouter implements Router {

  private static final Logger log = LoggerFactory.getLogger(TrustAnchorRouter.class);
  private final TrustAnchorFactory trustAnchorFactory;
  private final RouteFactory routeFactory;
  private final ServerResponseErrorHandler errorHandler;
  private final CacheSnapshotVersionLookup lookup;
  private final SubordinateFetchCache fetchCache;
  private final ObservationRegistry observationRegistry;

  /**
   * Constructor.
   * @param trustAnchorFactory  factory for creating trust anchor instances
   * @param routeFactory        factory for creating routes
   * @param errorHandler        handler for server response errors
   * @param lookup              lookup for scraped entities
   * @param fetchCache          cache for subordinate fetch responses
   * @param observationRegistry for recording observations
   */
  public TrustAnchorRouter(
      final TrustAnchorFactory trustAnchorFactory,
      final RouteFactory routeFactory,
      final ServerResponseErrorHandler errorHandler,
      final CacheSnapshotVersionLookup lookup,
      final SubordinateFetchCache fetchCache,
      final ObservationRegistry observationRegistry) {

    this.trustAnchorFactory = trustAnchorFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
    this.lookup = lookup;
    this.fetchCache = fetchCache;
    this.observationRegistry = observationRegistry;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(this.getRequestPredicate(source, "/fetch"),
            request -> this.handleFetchEntityStatement(source, request))
        .GET(this.getRequestPredicate(source, "/subordinate_listing"),
            request -> this.handleSubordinateListing(source, request));
  }

  private ServerResponse handleFetchEntityStatement(final CompositeRecordSource source, final ServerRequest request) {
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(), List.of("sub"));
      final FetchRequest fetchRequest = new FetchRequest(params.getFirst("sub"));
      final Long snapshot = this.lookup.getLatestSnapshotVersion();

      final Optional<ServerResponse> cached = this.handleCacheControl(request, fetchRequest, snapshot);
      if (cached.isPresent()) {
        this.tagObservation("/fetch", true);
        return cached.get();
      }

      final TrustAnchorProperties trustAnchorProperties = this.getPropertyByRequest(source, request, "/fetch");
      final TrustAnchor trustAnchor = this.trustAnchorFactory.create(trustAnchorProperties);
      final String response = trustAnchor.fetchEntityStatement(fetchRequest);
      this.fetchCache.put(snapshot, fetchRequest, response);
      this.tagObservation("/fetch", false);
      return ServerResponse.ok().body(response);
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private Optional<ServerResponse> handleCacheControl(
      final ServerRequest request, final FetchRequest fetchRequest, final Long snapshot) {
    final List<String> cacheControl = request.headers().header("cache-control");
    log.debug("Cache header was {} for trust anchor", cacheControl);
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.fetchCache.get(snapshot, fetchRequest)
          .map(response -> ServerResponse.ok().body(response));
    }
    return Optional.empty();
  }

  private void tagObservation(final String endpoint, final boolean cached) {
    final Observation observation = this.observationRegistry.getCurrentObservation();
    if (observation != null) {
      observation.lowCardinalityKeyValue("endpoint", endpoint)
          .lowCardinalityKeyValue("cached", String.valueOf(cached));
    }
  }

  private ServerResponse handleSubordinateListing(final CompositeRecordSource source, final ServerRequest request) {
    final TrustAnchorProperties properties = this.getPropertyByRequest(source, request, "/subordinate_listing");
    final TrustAnchor trustAnchor = this.trustAnchorFactory.create(properties);
    try {
      final MultiValueMap<String, String> params = request.params();
      this.tagObservation("/subordinate_listing", false);
      return ServerResponse.ok().body(trustAnchor.subordinateListing(new SubordinateListingRequest(
          params.getFirst("entity_type"),
          Optional.ofNullable(params.getFirst("trust_marked"))
              .map(Boolean::parseBoolean)
              .orElse(null),
          params.getFirst("trust_mark_type"),
          Optional.ofNullable(params.getFirst("intermediate"))
              .map(Boolean::parseBoolean)
              .orElse(null)
      )));
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private RequestPredicate getRequestPredicate(final CompositeRecordSource source, final String endpoint) {
    return request -> {
      return source.getTrustAnchorProperties().stream()
          .map(prop -> this.routeFactory.createRoute(prop.getEntityIdentifier(), endpoint))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    };
  }

  private TrustAnchorProperties getPropertyByRequest(
      final CompositeRecordSource source,
      final ServerRequest request,
      final String endpoint) {
    return source.getTrustAnchorProperties().stream()
        .filter(prop -> this.routeFactory.createRoute(prop.getEntityIdentifier(), endpoint).test(request))
        .findFirst()
        .get();
  }
}
