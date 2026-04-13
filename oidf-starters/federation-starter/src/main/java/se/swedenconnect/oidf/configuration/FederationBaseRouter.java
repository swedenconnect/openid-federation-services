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
package se.swedenconnect.oidf.configuration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.ModuleResponseCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.routing.ModuleRouter;
import se.swedenconnect.oidf.routing.Router;

import java.util.List;
import java.util.Optional;

/**
 * Base router that dispatches incoming requests to the appropriate {@link ModuleRouter}
 * based on virtual entity identity.
 *
 * @author Felix Hellman
 */
public class FederationBaseRouter implements Router {

  private final List<ModuleRouter> moduleRouters;
  private final ModuleResponseCache cache;
  private final CacheSnapshotVersionLookup lookup;

  /**
   * Constructor.
   *
   * @param moduleRouters routers for each active module
   * @param cache         response cache keyed by snapshot version and request URI
   * @param lookup        provides the current snapshot version
   */
  public FederationBaseRouter(
      final List<ModuleRouter> moduleRouters,
      final ModuleResponseCache cache,
      final CacheSnapshotVersionLookup lookup) {
    this.moduleRouters = moduleRouters;
    this.cache = cache;
    this.lookup = lookup;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route) {
    route.GET(request -> {
      final Optional<EntityRecord> entity = this.findEntityForRequest(source, request);
      return entity.isPresent()
          && this.moduleRouters.stream().anyMatch(router -> router.willHandleRequest(request, entity.get()));
    }, request -> {
      final String requestUri = request.uri().toASCIIString();
      final long snapshot = this.lookup.getLatestSnapshotVersion();
      final Optional<CachedResponse> cached = this.handleCacheControl(request, snapshot, requestUri);
      if (cached.isPresent()) {
        return this.toServerResponse(cached.get());
      }
      final EntityRecord entity = this.findEntityForRequest(source, request).get();
      final ModuleRouter module = this.moduleRouters.stream()
          .filter(router -> router.willHandleRequest(request, entity))
          .findFirst()
          .get();
      final CachedResponse response = module.handleRequest(request, entity);
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        this.cache.put(snapshot, requestUri, response);
      }
      return this.toServerResponse(response);
    });
  }

  private ServerResponse toServerResponse(final CachedResponse response) {
    return ServerResponse.status(response.statusCode())
        .contentType(MediaType.parseMediaType(response.contentType()))
        .body(response.body());
  }

  private Optional<CachedResponse> handleCacheControl(
      final ServerRequest request, final long snapshot, final String requestUri) {
    final List<String> cacheControl = request.headers().header("cache-control");
    if (cacheControl.isEmpty() || !"no-cache".equals(cacheControl.getFirst())) {
      return this.cache.get(snapshot, requestUri);
    }
    return Optional.empty();
  }

  private Optional<EntityRecord> findEntityForRequest(final CompositeRecordSource source,
      final ServerRequest request) {
    final String requestUri = request.uri().toASCIIString().split("\\?")[0];
    final String virtualEntityId;
    if (requestUri.endsWith("/.well-known/openid-federation")) {
      virtualEntityId = requestUri.substring(0, requestUri.length() - "/.well-known/openid-federation".length());
    } else {
      virtualEntityId = requestUri.substring(0, requestUri.lastIndexOf('/'));
    }
    return source.getEntityByVirtualEntityId(new EntityID(virtualEntityId)).or(() -> {
      return source.getEntity(new NodeKey(virtualEntityId));
    });
  }
}
