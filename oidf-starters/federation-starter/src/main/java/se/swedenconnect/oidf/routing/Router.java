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
package se.swedenconnect.oidf.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;

import java.net.URI;

/**
 * Router interface for adding routes.
 *
 * @author Felix Hellman
 */
public interface Router {
  /**
   * Registers routes for all entities in the given source.
   * Replaced by the {@link se.swedenconnect.oidf.configuration.FederationBaseRouter} +
   * {@link se.swedenconnect.oidf.routing.ModuleRouter} pattern, which performs direct entity
   * lookup instead of scanning all entities.
   *
   * @param source to read from
   * @param route to add routes to
   * @deprecated use {@link ModuleRouter#willHandleRequest} and {@link ModuleRouter#handleRequest} instead
   */
  @Deprecated(forRemoval = true)
  void evaluateEndpoints(final CompositeRecordSource source, final RouterFunctions.Builder route);

  /**
   * Extracts the entity ID from an incoming request URI by stripping the last path segment.
   * Replaced by virtual-entity-id lookup via
   * {@link se.swedenconnect.oidf.common.entity.entity.integration.RecordSource#getEntityByVirtualEntityId}.
   *
   * @param serverRequest to find entity id for
   * @return entity id
   * @deprecated derive the entity from the request URI via
   *     {@link se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource#getEntityByVirtualEntityId}
   *     instead
   */
  @Deprecated(forRemoval = true)
  default EntityID getEntityIdFromReuqest(final ServerRequest serverRequest) {
    final URI uri = serverRequest.uri();
    final String withoutQuery = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath();
    return new EntityID(withoutQuery.substring(0, withoutQuery.lastIndexOf('/')));
  }
}
