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
package se.swedenconnect.oidf.resolver.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.ResolverResponseCache;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.resolver.ResolverFactory;
import se.swedenconnect.oidf.routing.ModuleRouter;
import se.swedenconnect.oidf.routing.RequireParameters;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.Router;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;

import com.nimbusds.jose.shaded.gson.Gson;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Responsible for matching requests for any resolver module.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ResolverRouter implements Router, ModuleRouter {
  public static final Gson GSON = new Gson();
  private final ResolverFactory resolverFactory;
  private final RouteFactory routeFactory;
  private final ServerResponseErrorHandler errorHandler;
  private final ResolverResponseCache resolverResponseCache;
  private final CacheSnapshotVersionLookup lookup;
  private final ObservationRegistry observationRegistry;
  private final CompositeRecordSource source;

  /**
   * Constructor.
   *
   * @param resolverFactory factory for creating resolvers
   * @param routeFactory factory for creating routes
   * @param errorHandler handler for server response errors
   * @param resolverResponseCache cache for resolver responses
   * @param lookup lookup for scraped entities
   * @param observationRegistry for recording observations
   * @param source composite record source for entity lookups
   */
  public ResolverRouter(final ResolverFactory resolverFactory,
                        final RouteFactory routeFactory,
                        final ServerResponseErrorHandler errorHandler,
                        final ResolverResponseCache resolverResponseCache,
                        final CacheSnapshotVersionLookup lookup,
                        final ObservationRegistry observationRegistry,
                        final CompositeRecordSource source) {
    this.resolverFactory = resolverFactory;
    this.routeFactory = routeFactory;
    this.errorHandler = errorHandler;
    this.resolverResponseCache = resolverResponseCache;
    this.lookup = lookup;
    this.observationRegistry = observationRegistry;
    this.source = source;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
          return source.getResolverProperties().stream()
              .map(prop -> {
                return this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/resolve");
              }).reduce(p -> false, RequestPredicate::or)
              .test(request);
        }, request -> {
          try {
            final MultiValueMap<String, String> params = RequireParameters.validate(
                request.params(),
                List.of("sub", "trust_anchor")
            );

            if (params.containsKey("explain") && Boolean.parseBoolean(params.getFirst("explain"))) {
              final ResolverProperties resolverProperties = source.getResolverProperties().stream()
                  .filter(prop -> this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/resolve")
                      .test(request))
                  .findFirst()
                  .get();
              return ServerResponse.ok().body(this.resolverFactory.create(resolverProperties)
                  .explain(new ResolveRequest(
                  params.getFirst("sub"),
                  params.getFirst("trust_anchor"),
                  params.getFirst("entity_type"),
                  true
              )));
            }
            final ResolveRequest resolveRequest = new ResolveRequest(
                params.getFirst("sub"),
                params.getFirst("trust_anchor"),
                params.getFirst("entity_type"),
                false
            );
            final Long snapshot = this.lookup.getLatestSnapshotVersion();
            final Optional<ServerResponse> serverResponse =
                this.handleResolveResponseCacheControl(request, resolveRequest, snapshot);
            if (serverResponse.isPresent()) {
              this.tagObservation("/resolve", true);
              return serverResponse.get();
            }
            final ResolverProperties resolverProperties = source.getResolverProperties().stream()
                .filter(prop -> this.routeFactory.createRoute(new EntityID(prop.getEntityIdentifier()), "/resolve")
                    .test(request))
                .findFirst()
                .get();
            final String resolveResponse = this.resolverFactory.create(resolverProperties).resolve(resolveRequest);
            this.resolverResponseCache.put(snapshot, resolveRequest, resolveResponse);
            this.tagObservation("/resolve", false);
            return ServerResponse.ok().body(resolveResponse);
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
            this.tagObservation("/discovery", false);
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

  private void tagObservation(final String endpoint, final boolean cached) {
    final Observation observation = this.observationRegistry.getCurrentObservation();
    if (observation != null) {
      observation.lowCardinalityKeyValue("endpoint", endpoint)
          .lowCardinalityKeyValue("cached", String.valueOf(cached));
    }
  }

  private Optional<ServerResponse> handleResolveResponseCacheControl(final ServerRequest request,
      final ResolveRequest resolveRequest, final Long snapshot) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.resolverResponseCache.get(snapshot, resolveRequest)
          .map(status -> ServerResponse.ok().body(status));
    }
    return Optional.empty();
  }

  @Override
  public CachedResponse handleRequest(final ServerRequest request, final EntityRecord entity) {
    final boolean isResolve = entity.getFederationResolveEndpoint()
        .map(ep -> this.isResolveEndpoint(request, ep))
        .orElse(false);
    final String normalizedEndpoint = isResolve ? "/resolve" : "/discovery";
    final Observation observation = Observation.createNotStarted("oidf.resolver", this.observationRegistry)
        .lowCardinalityKeyValue("endpoint", normalizedEndpoint)
        .start();
    try {
      final Optional<ResolverProperties> resolverProperties = this.source.getResolverProperties().stream()
          .filter(property -> property.getEntityIdentifier().equalsIgnoreCase(entity.getEntityIdentifier().getValue()))
          .findFirst();
      final Resolver resolver = this.resolverFactory.create(resolverProperties.get());
      if (isResolve) {
        final ResolveRequest resolveRequest = this.createResolveRequest(request);
        final String response = resolver.resolve(resolveRequest);
        return new CachedResponse(response, "application/resolve-response+jwt", 200);
      }
      final DiscoveryRequest discoveryRequest = this.createDiscoveryRequest(request);
      final List<String> entities = resolver.discovery(discoveryRequest).supportedEntities();
      return new CachedResponse(GSON.toJson(entities), "application/json", 200);
    } catch (final FederationException e) {
      observation.error(e);
      final ServerResponse errorResponse = this.errorHandler.handle(e);
      return new CachedResponse(GSON.toJson(e.toJSONObject()), "application/json", errorResponse.statusCode().value());
    } finally {
      observation.stop();
    }
  }

  private ResolveRequest createResolveRequest(final ServerRequest request) throws FederationException {
    final MultiValueMap<String, String> params = RequireParameters.validate(
        request.params(), List.of("sub", "trust_anchor"));
    final boolean explain = Boolean.parseBoolean(params.getFirst("explain"));
    return new ResolveRequest(
        params.getFirst("sub"),
        params.getFirst("trust_anchor"),
        params.getFirst("entity_type"),
        explain
    );
  }

  private DiscoveryRequest createDiscoveryRequest(final ServerRequest request) throws FederationException {
    final MultiValueMap<String, String> params = RequireParameters.validate(
        request.params(), List.of("trust_anchor"));
    return new DiscoveryRequest(
        params.getFirst("trust_anchor"),
        params.get("entity_type"),
        params.get("trust_mark_type")
    );
  }

  @Override
  public boolean willHandleRequest(final ServerRequest request, final EntityRecord entity) {
    final Optional<String> resolveEndpoint = entity.getFederationResolveEndpoint();
    return resolveEndpoint.filter(s -> this.isResolveEndpoint(request, s)
                                       || this.isDiscoveryEndpoint(request, entity)).isPresent();
  }

  private boolean isResolveEndpoint(final ServerRequest request, final String resolveEndpoint) {
    return request.uri().toASCIIString().split("\\?")[0].equals(resolveEndpoint);
  }

  private boolean isDiscoveryEndpoint(final ServerRequest request, final EntityRecord entity) {
    return entity.getVirtualEntityId() != null
        && request.uri().toASCIIString().contains(entity.getVirtualEntityId().getValue())
        && request.path().endsWith("/discovery");
  }
}
