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
import se.digg.oidfed.common.entity.integration.federation.FetchRequest;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.service.trustanchor.TrustAnchorFactory;

import java.util.List;
import java.util.Optional;

@Component
public class TrustAnchorRouter implements Router {

  private final TrustAnchorFactory trustAnchorFactory;
  private final RouteFactory routeFactory;
  private final ServerResponseErrorHandler errorHandler;

  public TrustAnchorRouter(final TrustAnchorFactory trustAnchorFactory, final RouteFactory routeFactory, final ServerResponseErrorHandler errorHandler) {
    this.trustAnchorFactory = trustAnchorFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    source.getTrustAnchorProperties()
        .stream().map(this.trustAnchorFactory::create)
        .forEach(ta -> {
          final RequestPredicate fetchRoute = this.routeFactory.createRoute(ta.getEntityId(), "/fetch");
          route.GET(fetchRoute, request -> {
            try {
              final MultiValueMap<String, String> params = RequireParameters.validate(
                  request.params(), List.of("sub"));
              return ServerResponse.ok().body(ta.fetchEntityStatement(
                  new FetchRequest(
                      params.getFirst("sub")
                  )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
          final RequestPredicate subordinateListing = this.routeFactory.createRoute(ta.getEntityId(),
              "/subordinate_listing");
          route.GET(subordinateListing, request -> {
            try {
              final MultiValueMap<String, String> params = request.params();
              return ServerResponse.ok().body(ta.subordinateListing(new SubordinateListingRequest(
                  params.getFirst("entity_type"),
                  Optional.ofNullable(params.getFirst("trust_marked"))
                      .map(Boolean::parseBoolean)
                      .orElse(null),
                  params.getFirst("trust_mark_id"),
                  Optional.ofNullable(params.getFirst("intermediate"))
                      .map(Boolean::parseBoolean)
                      .orElse(null)
              )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
        });
  }
}
