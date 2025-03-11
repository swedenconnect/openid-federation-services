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
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkListingRequest;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerFactory;
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
    source.getTrustMarkIssuerProperties()
        .stream()
        .map(this.factory::create)
        .forEach(tmi -> {
          final RequestPredicate trustMarkRoute = this.routeFactory.createRoute(tmi.getEntityId(), "/trust_mark");
          route.GET(trustMarkRoute, request -> {
            try {
              final MultiValueMap<String, String> params = request.params();
              return ServerResponse.ok().body(tmi.trustMark(new TrustMarkRequest(
                  params.getFirst("trust_mark_id"),
                  params.getFirst("sub")
              )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
          final RequestPredicate trustMarkStatusRoute = this.routeFactory.createRoute(tmi.getEntityId(),
              "/trust_mark_status");
          route.GET(trustMarkStatusRoute, request -> {
            try {
              final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
                  List.of("trust_mark_id", "sub"));
              return ServerResponse.ok().body(tmi.trustMarkStatus(new TrustMarkStatusRequest(
                  params.getFirst("trust_mark_id"),
                  params.getFirst("sub"),
                  Optional.ofNullable(params.getFirst("iat"))
                      .map(Long::parseLong)
                      .orElse(null)
              )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
          final RequestPredicate trustMarkListingRoute = this.routeFactory.createRoute(tmi.getEntityId(), "/trust_mark_listing");
          route.GET(trustMarkListingRoute, request -> {
            try {
              final MultiValueMap<String, String> params = RequireParameters.validate(request.params(),
                  List.of("trust_mark_id"));
              return ServerResponse.ok().body(
                  tmi.trustMarkListing(new TrustMarkListingRequest(
                      params.getFirst("trust_mark_id"),
                      params.getFirst("sub")
                  ))
              );
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
        });
  }
}
