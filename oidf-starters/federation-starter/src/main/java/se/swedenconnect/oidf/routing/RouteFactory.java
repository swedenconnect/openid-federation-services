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
package se.swedenconnect.oidf.routing;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicate;
import se.swedenconnect.oidf.FederationProperties;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Factory class for constructing routes.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class RouteFactory {

  private final ServletContext context;
  private final RouterProperties properties;

  /**
   * Constructor.
   *
   * @param context
   * @param properties
   */
  public RouteFactory(
      final ServletContext context,
      final FederationProperties properties) {
    this.context = context;
    final RouterProperties defaultProperties = new RouterProperties();
    defaultProperties.setMode(RouterProperties.DomainEvaluationMode.IGNORING);
    defaultProperties.setAllowedDomains(List.of());
    this.properties = Optional.ofNullable(properties.getRouting())
        .orElse(defaultProperties);
  }

  /**
   * Creates a route for an entity.
   *
   * @param entityID for this route
   * @param endpoint for this route
   * @return route predicate
   */
  public RequestPredicate createRoute(
      final EntityID entityID,
      final String endpoint) {
    return this.createRoute(entityID, endpoint, true);
  }

/**
 * Creates a route for an entity.
 *
 * @param entityID for this route
 * @param endpoint for this route
 * @param logOnWarning log warning, disable this if an alternate route is feasible
 * @return route predicate
 */
public RequestPredicate createRoute(
    final EntityID entityID,
    final String endpoint,
    final Boolean logOnWarning) {
  final URI uri = URI.create(entityID.getValue());
  final String path = uri.getPath();
  final String host = uri.getHost();
  if (!uri.toString().contains(this.context.getContextPath()) && logOnWarning) {
    log.warn("Could not create route {}/{}, path does not contain server context path {}",
        entityID.getValue(),
        endpoint,
        this.context.getContextPath()
    );
    return r -> false;
  }
  final String contextAwarePath = path.replace(this.context.getContextPath(), "");
  final String internalEndpoint = "%s%s".formatted(contextAwarePath, endpoint);
  final RequestPredicate hostPredicate = r -> Optional.ofNullable(r.headers().header("host").getFirst()).orElse(
      "").equals(host);
  final RequestPredicate pathPredicate = r -> r.path().equals(internalEndpoint);
  if (this.properties.getMode().equals(RouterProperties.DomainEvaluationMode.STRICT)) {
    return hostPredicate.and(pathPredicate);
  }
  return pathPredicate;
}

/**
 * @param urlOrEndpoint to create endpoint for
 * @return predicate
 */
public RequestPredicate createAlternateRoute(final String urlOrEndpoint) {
  if (!urlOrEndpoint.contains(this.context.getContextPath())) {
    log.warn("Could not create alternate route {}, path does not contain server context path {}",
        urlOrEndpoint,
        this.context.getContextPath()
    );
  }
  if (urlOrEndpoint.startsWith("/")) {
    return r -> r.path().equals(urlOrEndpoint);
  }
  return r -> r.uri().equals(URI.create(urlOrEndpoint));
}
}
