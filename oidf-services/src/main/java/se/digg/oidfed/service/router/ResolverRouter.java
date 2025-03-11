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
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.resolver.DiscoveryRequest;
import se.digg.oidfed.service.resolver.ResolverFactory;

import java.util.List;

@Slf4j
@Component
public class ResolverRouter implements Router {
  private final ResolverFactory resolverFactory;
  private final RouteFactory routeFactory;
  private final ServerResponseErrorHandler errorHandler;

  public ResolverRouter(final ResolverFactory resolverFactory,
                        final RouteFactory routeFactory,
                        final ServerResponseErrorHandler errorHandler) {
    this.resolverFactory = resolverFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    source.getResolverProperties().stream()
        .map(this.resolverFactory::create)
        .forEach(resolver -> {
          final RequestPredicate resolve = this.routeFactory.createRoute(resolver.getEntityId(), "/resolve");
          route.GET(resolve, r -> {
            try {
              final MultiValueMap<String, String> params = RequireParameters.validate(
                  r.params(),
                  List.of("sub", "trust_anchor")
              );
              return ServerResponse.ok().body(resolver.resolve(new ResolveRequest(
                  params.getFirst("sub"),
                  params.getFirst("trust_anchor"),
                  params.getFirst("entity_type")
              )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
          final RequestPredicate discovery = this.routeFactory.createRoute(resolver.getEntityId(), "/discovery");
          route.GET(discovery, r -> {
            try {
              final MultiValueMap<String, String> params = RequireParameters.validate(r.params(), List.of("trust_anchor"));
              return ServerResponse.ok().body(resolver.discovery(new DiscoveryRequest(
                  params.getFirst("trust_anchor"),
                  params.get("entity_type"),
                  params.get("trust_mark_id")
              )));
            } catch (final FederationException e) {
              return this.errorHandler.handle(e);
            }
          });
        });
  }
}
