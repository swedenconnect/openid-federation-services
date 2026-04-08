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
package se.swedenconnect.oidf.trustmarkissuer.starter.routing;

import com.nimbusds.jose.shaded.gson.Gson;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkCache;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkStatusCache;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.routing.ModuleRouter;
import se.swedenconnect.oidf.routing.RequireParameters;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.Router;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;
import se.swedenconnect.oidf.trustmarkissuer.TrustMarkIssuer;
import se.swedenconnect.oidf.trustmarkissuer.TrustMarkRequest;
import se.swedenconnect.oidf.trustmarkissuer.TrustMarkStatusRequest;
import se.swedenconnect.oidf.trustmarkissuer.starter.TrustMarkIssuerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Router responsible for matching trust mark issuer requests to a trust mark issuer module.
 *
 * @author Felix Hellman
 */
public class TrustMarkIssuerRouter implements Router, ModuleRouter {

  private static final Logger log = LoggerFactory.getLogger(TrustMarkIssuerRouter.class);
  public static final Gson GSON = new Gson();
  private final RouteFactory routeFactory;
  private final TrustMarkIssuerFactory factory;
  private final ServerResponseErrorHandler errorHandler;
  private final CacheSnapshotVersionLookup lookup;
  private final TrustMarkStatusCache trustMarkStatusCache;
  private final TrustMarkCache trustMarkCache;
  private final ObservationRegistry observationRegistry;
  private final CompositeRecordSource source;

  /**
   * Constructor.
   *
   * @param routeFactory         route factory
   * @param factory              trust mark issuer factory
   * @param errorHandler         handler for server response errors
   * @param lookup               lookup for scraped entities
   * @param trustMarkStatusCache cache for trust mark status responses
   * @param trustMarkCache       cache for trust mark responses
   * @param observationRegistry  for recording observations
   * @param source               record source for looking up properties
   */
  public TrustMarkIssuerRouter(
      final RouteFactory routeFactory,
      final TrustMarkIssuerFactory factory,
      final ServerResponseErrorHandler errorHandler,
      final CacheSnapshotVersionLookup lookup,
      final TrustMarkStatusCache trustMarkStatusCache,
      final TrustMarkCache trustMarkCache,
      final ObservationRegistry observationRegistry,
      final CompositeRecordSource source) {
    this.routeFactory = routeFactory;
    this.factory = factory;
    this.errorHandler = errorHandler;
    this.lookup = lookup;
    this.trustMarkStatusCache = trustMarkStatusCache;
    this.trustMarkCache = trustMarkCache;
    this.observationRegistry = observationRegistry;
    this.source = source;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(this.getRequestPredicate(source, "/trust_mark"),
            request -> this.handleTrustMarkRequest(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_status"),
            request -> this.handleTrustMarkStatus(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_listing"),
            request -> this.handleTrustMarkListing(source, request));
  }

  @Override
  public CachedResponse handleRequest(final ServerRequest request, final EntityRecord entity) {
    final String normalizedEndpoint;
    if (this.isTrustMarkEndpoint(request, entity)) {
      normalizedEndpoint = "/trust_mark";
    } else if (this.isTrustMarkStatusEndpoint(request, entity)) {
      normalizedEndpoint = "/trust_mark_status";
    } else {
      normalizedEndpoint = "/trust_mark_listing";
    }
    final Observation observation = Observation.createNotStarted("oidf.trust.mark.issuer", this.observationRegistry)
        .lowCardinalityKeyValue("endpoint", normalizedEndpoint)
        .start();
    try {
      final TrustMarkIssuerProperties properties = this.source.getTrustMarkIssuerProperties().stream()
          .filter(p -> p.entityIdentifier().getValue().equals(entity.getEntityIdentifier().getValue()))
          .findFirst()
          .get();
      final TrustMarkIssuer trustMarkIssuer = this.factory.create(properties);
      if (this.isTrustMarkEndpoint(request, entity)) {
        final MultiValueMap<String, String> params = request.params();
        final String response = trustMarkIssuer.trustMark(
            new TrustMarkRequest(params.getFirst("trust_mark_type"), params.getFirst("sub")));
        return new CachedResponse(response, "application/trust-mark+jwt", 200);
      }
      if (this.isTrustMarkStatusEndpoint(request, entity)) {
        final MultiValueMap<String, String> params =
            RequireParameters.validate(request.params(), List.of("trust_mark"));
        final String trustMarkStatus =
            trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest(params.getFirst("trust_mark")));
        return new CachedResponse(trustMarkStatus, "application/trust-mark-status-response+jwt", 200);
      }
      final MultiValueMap<String, String> params =
          RequireParameters.validate(request.params(), List.of("trust_mark_type"));
      final List<String> listing = trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(
          params.getFirst("trust_mark_type"),
          params.getFirst("sub")
      ));
      return new CachedResponse(GSON.toJson(listing), "application/json", 200);
    } catch (final FederationException e) {
      observation.error(e);
      final ServerResponse errorResponse = this.errorHandler.handle(e);
      return new CachedResponse(GSON.toJson(e.toJSONObject()), "application/json", errorResponse.statusCode().value());
    } finally {
      observation.stop();
    }
  }

  @Override
  public boolean willHandleRequest(final ServerRequest request, final EntityRecord entity) {
    return this.isTrustMarkEndpoint(request, entity)
           || this.isTrustMarkStatusEndpoint(request, entity)
           || this.isTrustMarkListingEndpoint(request, entity);
  }

  private boolean isTrustMarkEndpoint(final ServerRequest request, final EntityRecord entity) {
    return entity.getFederationTrustMarkEndpoint()
        .map(ep -> request.uri().toASCIIString().split("\\?")[0].equals(ep))
        .orElse(false);
  }

  private boolean isTrustMarkStatusEndpoint(final ServerRequest request, final EntityRecord entity) {
    return entity.getFederationTrustMarkStatusEndpoint()
        .map(ep -> request.uri().toASCIIString().split("\\?")[0].equals(ep))
        .orElse(false);
  }

  private boolean isTrustMarkListingEndpoint(final ServerRequest request, final EntityRecord entity) {
    return entity.getFederationTrustMarkListingEndpoint()
        .map(ep -> request.uri().toASCIIString().split("\\?")[0].equals(ep))
        .orElse(false);
  }

  private ServerResponse handleTrustMark(final ServerRequest request, final TrustMarkIssuer trustMarkIssuer)
      throws FederationException {
    final MultiValueMap<String, String> params = request.params();
    final String trustMarkType = params.getFirst("trust_mark_type");
    final String sub = params.getFirst("sub");
    final Long snapshot = this.lookup.getLatestSnapshotVersion();
    final Optional<ServerResponse> cached = this.handleTrustMarkCacheControl(request, trustMarkType, sub, snapshot);
    if (cached.isPresent()) {
      this.tagObservation("/trust_mark", true);
      return cached.get();
    }
    final String response = trustMarkIssuer.trustMark(new TrustMarkRequest(trustMarkType, sub));
    this.trustMarkCache.put(snapshot, trustMarkType, sub, response);
    this.tagObservation("/trust_mark", false);
    return ServerResponse.ok().body(response);
  }

  private ServerResponse handleTrustMarkStatus(final ServerRequest request, final TrustMarkIssuer trustMarkIssuer)
      throws FederationException {
    final MultiValueMap<String, String> params = RequireParameters.validate(request.params(), List.of("trust_mark"));
    final String trustMarkJwt = params.getFirst("trust_mark");
    final Long snapshot = this.lookup.getLatestSnapshotVersion();
    final Optional<ServerResponse> cached = this.handleTrustMarkStatusCacheControl(request, trustMarkJwt, snapshot);
    if (cached.isPresent()) {
      this.tagObservation("/trust_mark_status", true);
      return cached.get();
    }
    final String trustMarkStatus = trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest(trustMarkJwt));
    this.trustMarkStatusCache.put(snapshot, trustMarkJwt, trustMarkStatus);
    this.tagObservation("/trust_mark_status", false);
    return ServerResponse.ok()
        .contentType(MediaType.parseMediaType("application/trust-mark-status-response+jwt"))
        .body(trustMarkStatus);
  }

  private ServerResponse handleTrustMarkListing(final ServerRequest request, final TrustMarkIssuer trustMarkIssuer)
      throws FederationException {
    final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
        List.of("trust_mark_type"));
    this.tagObservation("/trust_mark_listing", false);
    return ServerResponse.ok().body(trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(
        params.getFirst("trust_mark_type"),
        params.getFirst("sub")
    )));
  }

  private ServerResponse handleTrustMarkStatus(final CompositeRecordSource source, final ServerRequest request) {
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
          List.of("trust_mark"));
      final String trustMarkJwt = params.getFirst("trust_mark");
      final Long snapshot = this.lookup.getLatestSnapshotVersion();
      final Optional<ServerResponse> serverResponse =
          this.handleTrustMarkStatusCacheControl(request, trustMarkJwt, snapshot);
      if (serverResponse.isPresent()) {
        this.tagObservation("/trust_mark_status", true);
        return serverResponse.get();
      }
      final TrustMarkIssuerProperties propertyByRequest =
          this.getPropertyByRequest(source, request, "/trust_mark_status");
      final TrustMarkIssuer trustMarkIssuer = this.factory.create(propertyByRequest);
      try {
        final String trustMarkStatus = trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest(trustMarkJwt));
        this.trustMarkStatusCache.put(snapshot, trustMarkJwt, trustMarkStatus);
        this.tagObservation("/trust_mark_status", false);
        return ServerResponse.ok()
            .contentType(MediaType.parseMediaType("application/trust-mark-status-response+jwt"))
            .body(trustMarkStatus);
      } catch (final FederationException e) {
        return this.errorHandler.handle(e);
      }
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private ServerResponse handleTrustMarkListing(final CompositeRecordSource source, final ServerRequest request) {
    final TrustMarkIssuerProperties property = this.getPropertyByRequest(source, request, "/trust_mark_listing");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(property);
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
          List.of("trust_mark_type"));
      this.tagObservation("/trust_mark_listing", false);
      return ServerResponse.ok().body(trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(
          params.getFirst("trust_mark_type"),
          params.getFirst("sub")
      )));
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private ServerResponse handleTrustMarkRequest(final CompositeRecordSource source, final ServerRequest request) {
    final MultiValueMap<String, String> params = request.params();
    final String trustMarkType = params.getFirst("trust_mark_type");
    log.debug("Handling trust mark request {}", params);
    final String sub = params.getFirst("sub");

    final TrustMarkIssuerProperties property = this.getPropertyByRequest(source, request, "/trust_mark");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(property);
    try {
      log.debug("Using fresh trust mark for {} {} {}", params, property, request.headers());
      final String response = trustMarkIssuer.trustMark(new TrustMarkRequest(trustMarkType, sub));
      this.tagObservation("/trust_mark", false);
      return ServerResponse.ok().body(response);
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private void tagObservation(final String endpoint, final boolean cached) {
    final Observation observation = this.observationRegistry.getCurrentObservation();
    if (observation != null) {
      observation.lowCardinalityKeyValue("endpoint", endpoint)
          .lowCardinalityKeyValue("cached", String.valueOf(cached));
    }
  }

  private Optional<ServerResponse> handleTrustMarkCacheControl(
      final ServerRequest request, final String trustMarkType, final String sub, final Long snapshot) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.trustMarkCache.get(snapshot, trustMarkType, sub)
          .map(response -> ServerResponse.ok().body(response));
    }
    return Optional.empty();
  }

  private Optional<ServerResponse> handleTrustMarkStatusCacheControl(final ServerRequest request,
                                                                     final String trustMarkJwt, final Long snapshot) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.trustMarkStatusCache.get(snapshot, trustMarkJwt)
          .map(status -> ServerResponse.ok().body(status));
    }
    return Optional.empty();
  }

  private RequestPredicate getRequestPredicate(final CompositeRecordSource source, final String endpoint) {
    return request -> source.getTrustMarkIssuerProperties().stream()
        .map(prop -> this.routeFactory.createRoute(prop.entityIdentifier(), endpoint))
        .reduce(p -> false, RequestPredicate::or)
        .test(request);
  }

  private TrustMarkIssuerProperties getPropertyByRequest(final CompositeRecordSource source,
                                                         final ServerRequest request,
                                                         final String endpoint) {
    return source.getTrustMarkIssuerProperties().stream()
        .filter(prop -> this.routeFactory.createRoute(prop.entityIdentifier(), endpoint).test(request))
        .findFirst()
        .get();
  }
}
