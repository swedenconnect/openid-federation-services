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
package se.swedenconnect.oidf.trustanchor.routing;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntityLookup;
import se.swedenconnect.oidf.routing.RequireParameters;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.Router;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;
import se.swedenconnect.oidf.trustanchor.ScrapedTrustAnchor;
import se.swedenconnect.oidf.trustanchor.TrustAnchor;
import se.swedenconnect.oidf.trustanchor.TrustAnchorFactory;

import java.util.List;
import java.util.Objects;
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
  private final ScrapedEntityLookup lookup;

  /**
   * Constructor.
   * @param trustAnchorFactory factory for creating trust anchor instances
   * @param routeFactory       factory for creating routes
   * @param errorHandler       handler for server response errors
   * @param lookup             lookup for scraped entities
   */
  public TrustAnchorRouter(
      final TrustAnchorFactory trustAnchorFactory,
      final RouteFactory routeFactory,
      final ServerResponseErrorHandler errorHandler,
      final ScrapedEntityLookup lookup) {

    this.trustAnchorFactory = trustAnchorFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
    this.lookup = lookup;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(this.getRequestPredicate(source, "/fetch"),
            request -> this.handleFetchEntityStatement(source, request))
        .GET(this.getRequestPredicate(source, "/subordinate_listing"),
            request -> this.handleSubordinateListing(source, request));
  }

  private ServerResponse handleFetchEntityStatement(final CompositeRecordSource source, final ServerRequest request) {


    final TrustAnchorProperties trustAnchorProperties = this.getPropertyByRequest(source, request, "/fetch");
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(), List.of("sub"));
      final FetchRequest fetchRequest = new FetchRequest(params.getFirst("sub"));

      return this.handleCacheControl(request, fetchRequest)
          .orElseGet(() -> {
            try {
              final TrustAnchor trustAnchor = this.trustAnchorFactory.create(trustAnchorProperties);
              return ServerResponse.ok().body(trustAnchor.fetchEntityStatement(fetchRequest));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });

    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private Optional<ServerResponse> handleCacheControl(
      final ServerRequest request, final FetchRequest fetchRequest)
      throws InvalidIssuerException, NotFoundException {
    final List<String> cacheControl = request.headers().header("cache-control");
    log.debug("Cache header was {} for trust anchor", cacheControl);
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      final Optional<ScrapedEntity> trustMarkIssuer =
          this.lookup.findTrustAnchorByEntityId(this.getEntityIdFromReuqest(request).getValue());
      if (trustMarkIssuer.isPresent()) {
        return Optional.of(ServerResponse.ok()
            .body(new ScrapedTrustAnchor(trustMarkIssuer.get()).fetchEntityStatement(fetchRequest)));
      }
    }
    return Optional.empty();
  }

  private ServerResponse handleSubordinateListing(final CompositeRecordSource source, final ServerRequest request) {
    final TrustAnchorProperties properties = this.getPropertyByRequest(source, request, "/subordinate_listing");
    final TrustAnchor trustAnchor = this.trustAnchorFactory.create(properties);
    try {
      final MultiValueMap<String, String> params = request.params();
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
