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

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkListingRequest;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkIssuerProperties;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerFactory;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkRequest;
import se.digg.oidfed.trustmarkissuer.TrustMarkStatusRequest;

import java.util.List;
import java.util.Optional;

@Component
public class TrustMarkIssuerRouter implements Router {

  private final RouteFactory routeFactory;
  private final TrustMarkIssuerFactory factory;
  private final ServerResponseErrorHandler errorHandler;

  public TrustMarkIssuerRouter(final RouteFactory routeFactory, final TrustMarkIssuerFactory factory, final ServerResponseErrorHandler errorHandler) {
    this.routeFactory = routeFactory;
    this.factory = factory;
    this.errorHandler = errorHandler;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(this.getRequestPredicate(source, "/trust_mark"), request -> this.handleTrustMarkRequest(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_status"), request -> this.handleTrustMarkStatus(source, request))
        .GET(this.getRequestPredicate(source, "/trust_mark_listing"), request -> this.handleTrustMarkListing(source,
            request));
  }

  private ServerResponse handleTrustMarkStatus(final CompositeRecordSource source, final ServerRequest request) {
    final TrustMarkIssuerProperties propertyByRequest = this.getPropertyByRequest(source, request, "/trust_mark_status");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(propertyByRequest);
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
          List.of("trust_mark_id", "sub"));
      return ServerResponse.ok().body(trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest(
          params.getFirst("trust_mark_id"),
          params.getFirst("sub"),
          Optional.ofNullable(params.getFirst("iat"))
              .map(Long::parseLong)
              .orElse(null)
      )));
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private ServerResponse handleTrustMarkListing(final CompositeRecordSource source, final ServerRequest request) {
    final TrustMarkIssuerProperties property = this.getPropertyByRequest(source, request, "/trust_mark_listing");
    final TrustMarkIssuer trustMarkIssuer = this.factory.create(property);
    try {
      final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
          List.of("trust_mark_id"));
      return ServerResponse.ok().body(
          trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(
              params.getFirst("trust_mark_id"),
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
    try {
      final MultiValueMap<String, String> params = request.params();
      return ServerResponse.ok().body(trustMarkIssuer.trustMark(new TrustMarkRequest(
          params.getFirst("trust_mark_id"),
          params.getFirst("sub")
      )));
    } catch (final FederationException e) {
      return this.errorHandler.handle(e);
    }
  }

  private RequestPredicate getRequestPredicate(final CompositeRecordSource source, final String endpoint) {
    return request -> {
      return source.getTrustMarkIssuerProperties().stream()
          .map(prop -> this.routeFactory.createRoute(prop.issuerEntityId(), endpoint))
          .reduce(p -> false, RequestPredicate::or)
          .test(request);
    };
  }

  private TrustMarkIssuerProperties getPropertyByRequest(final CompositeRecordSource source, final ServerRequest request,
                                                         final String endpoint) {
    return source.getTrustMarkIssuerProperties().stream()
        .filter(prop -> this.routeFactory.createRoute(prop.issuerEntityId(), endpoint).test(request))
        .findFirst()
        .get();
  }
}
