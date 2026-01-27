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
package se.swedenconnect.oidf.resolver.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.resolver.ResolverFactory;
import se.swedenconnect.oidf.routing.RequireParameters;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.Router;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;

import java.util.List;

/**
 * Responsible for matching requests for any resolver module.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ResolverRouter implements Router {
  private final ResolverFactory resolverFactory;
  private final RouteFactory routeFactory;
  private final ServerResponseErrorHandler errorHandler;

  /**
   * Constructor.
   * @param resolverFactory
   * @param routeFactory
   * @param errorHandler
   */
  public ResolverRouter(final ResolverFactory resolverFactory,
                        final RouteFactory routeFactory,
                        final ServerResponseErrorHandler errorHandler) {
    this.resolverFactory = resolverFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
  }

  @Override
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
          return source.getResolverProperties().stream()
              .map(prop -> {
                return this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/resolve");
              }).reduce(p -> false, RequestPredicate::or)
              .test(request);
        }, request -> {
          final ResolverProperties resolverProperties = source.getResolverProperties().stream()
              .filter(prop -> this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/resolve")
                  .test(request))
              .findFirst()
              .get();
          final Resolver resolver = this.resolverFactory.create(resolverProperties);
          try {
            final MultiValueMap<String, String> params = RequireParameters.validate(
                request.params(),
                List.of("sub", "trust_anchor")
            );

            if (params.containsKey("explain") && Boolean.parseBoolean(params.getFirst("explain"))) {
              return ServerResponse.ok().body(resolver.explain(new ResolveRequest(
                  params.getFirst("sub"),
                  params.getFirst("trust_anchor"),
                  params.getFirst("entity_type"),
                  true
              )));
            }
            return ServerResponse.ok().body(resolver.resolve(new ResolveRequest(
                params.getFirst("sub"),
                params.getFirst("trust_anchor"),
                params.getFirst("entity_type"),
                false
            )));
          } catch (final FederationException e) {
            return this.errorHandler.handle(e);
          }
        })
        .GET(request -> {
          return source.getResolverProperties().stream()
              .map(prop -> {
                return this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/discovery");
              }).reduce(p -> false, RequestPredicate::or)
              .test(request);
        }, request -> {
          final ResolverProperties resolverProperties = source.getResolverProperties().stream()
              .filter(prop -> this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/discovery")
                  .test(request))
              .findFirst()
              .get();
          final Resolver resolver = this.resolverFactory.create(resolverProperties);
          try {
            final MultiValueMap<String, String> params = RequireParameters.validate(request.params(), List.of(
                "trust_anchor"));
            return ServerResponse.ok().body(resolver.discovery(new DiscoveryRequest(
                params.getFirst("trust_anchor"),
                params.get("entity_type"),
                params.get("trust_mark_type")
            )).supportedEntities());
          } catch (final FederationException e) {
            return this.errorHandler.handle(e);
          }
        });
  }
}
