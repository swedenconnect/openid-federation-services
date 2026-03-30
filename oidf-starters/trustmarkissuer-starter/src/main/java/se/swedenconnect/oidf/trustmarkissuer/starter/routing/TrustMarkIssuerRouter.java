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
package se.swedenconnect.oidf.trustmarkissuer.starter.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkStatusCache;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntityLookup;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedTrustMarkInfo;
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
@Component
public class TrustMarkIssuerRouter implements Router {

  private static final Logger log = LoggerFactory.getLogger(TrustMarkIssuerRouter.class);
  private final RouteFactory routeFactory;
  private final TrustMarkIssuerFactory factory;
  private final ServerResponseErrorHandler errorHandler;
  private final ScrapedEntityLookup lookup;
  private final TrustMarkStatusCache trustMarkStatusCache;

  /**
   * Constructor.
   *
   * @param routeFactory route factory
   * @param factory      trust mark issuer factory
   * @param errorHandler handler for server response errors
   * @param lookup       lookup for scraped entities
   * @param cache        to store responses
   */
  public TrustMarkIssuerRouter(
      final RouteFactory routeFactory,
      final TrustMarkIssuerFactory factory,
      final ServerResponseErrorHandler errorHandler,
      final ScrapedEntityLookup lookup,
      final TrustMarkStatusCache cache) {
    this.routeFactory = routeFactory;
    this.factory = factory;
    this.errorHandler = errorHandler;
    this.lookup = lookup;
    this.trustMarkStatusCache = cache;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(this.getRequestPredicate(source, "/trust_mark"),
            request -> this.handleTrustMarkRequest(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_status"),
            request -> this.handleTrustMarkStatus(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_listing"), request -> this.handleTrustMarkListing(source,
            request));
  }

  private ServerResponse handleTrustMarkStatus(final CompositeRecordSource source, final ServerRequest request) {
    final TrustMarkIssuerProperties propertyByRequest
        = this.getPropertyByRequest(source, request, "/trust_mark_status");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(propertyByRequest);
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
          List.of("trust_mark"));
      final String trustMarkJwt = params.getFirst("trust_mark");
      final Long snapshot = this.lookup.getLatestSnapshotVersion();
      final Optional<ServerResponse> serverResponse =
          this.handleTrustMarkStatusCacheControl(request, trustMarkJwt, snapshot);
      if (serverResponse.isPresent()) {
        return serverResponse.get();
      }
      try {
        final String trustMarkStatus = trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest(trustMarkJwt));
        this.trustMarkStatusCache.put(snapshot, trustMarkJwt, trustMarkStatus);
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
      return ServerResponse.ok().body(
          trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(
              params.getFirst("trust_mark_type"),
              params.getFirst("sub")
          ))
      );
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private ServerResponse handleTrustMarkRequest(final CompositeRecordSource source, final ServerRequest request) {
    final TrustMarkIssuerProperties property = this.getPropertyByRequest(source, request, "/trust_mark");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(property);
    final MultiValueMap<String, String> params = request.params();
    final String trustMarkType = params.getFirst("trust_mark_type");
    log.debug("Handling trust mark request {}", params);
    final String sub = params.getFirst("sub");
    return this.handleTrustMarkCacheControl(request, trustMarkType, sub)
        .orElseGet(() -> {
          try {
            log.debug("Using fresh trust mark for {} {} {}", params, property, request.headers());
            return ServerResponse.ok().body(trustMarkIssuer.trustMark(new TrustMarkRequest(trustMarkType, sub)));
          } catch (final FederationException e) {
            return this.errorHandler.handle(e);
          }
        });
  }

  private Optional<ServerResponse> handleTrustMarkCacheControl(
      final ServerRequest request, final String trustMarkType, final String sub) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      final Optional<ScrapedEntity> entity =
          this.lookup.findTrustMarkIssuer(this.getEntityIdFromReuqest(request).getValue());
      if (entity.isPresent()) {
        final Optional<ScrapedTrustMarkInfo> info =
            entity.get().getTrustMarkIssuer().trustMarkInfo(trustMarkType, sub);
        if (info.isPresent()) {
          return Optional.of(ServerResponse.ok().body(info.get().trustMark().serialize()));
        }
      }
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
    return request -> {
      return source.getTrustMarkIssuerProperties().stream()
          .map(prop -> this.routeFactory.createRoute(prop.entityIdentifier(), endpoint))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    };
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
