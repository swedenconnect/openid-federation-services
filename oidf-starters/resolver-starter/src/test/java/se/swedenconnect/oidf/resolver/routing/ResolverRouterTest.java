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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;
import io.micrometer.observation.ObservationRegistry;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import org.springframework.util.LinkedMultiValueMap;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.resolver.ResolverFactory;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class ResolverRouterTest {

  private static final String ENTITY_ID = "https://myentity.com/resolver";
  private static final String VIRTUAL_ENTITY_ID = "https://myentity.com/virtual/resolver";

  private final ResolverRouter router = new ResolverRouter(null, null, null, null, null, null, null);

  @Test
  void discoveryIsHandledForEntityWithoutVirtualEntityId() {
    Assertions.assertTrue(this.router.willHandleRequest(
        request(ENTITY_ID + "/discovery?trust_anchor=https://myentity.com/ta"),
        record(null)));
  }

  @Test
  void resolveIsHandledForEntityWithoutVirtualEntityId() {
    Assertions.assertTrue(this.router.willHandleRequest(
        request(ENTITY_ID + "/resolve?sub=https://myentity.com/rp&trust_anchor=https://myentity.com/ta"),
        record(null)));
  }

  @Test
  void discoveryIsHandledOnVirtualEntityId() {
    Assertions.assertTrue(this.router.willHandleRequest(
        request(VIRTUAL_ENTITY_ID + "/discovery?trust_anchor=https://myentity.com/ta"),
        record(VIRTUAL_ENTITY_ID)));
  }

  @Test
  void discoveryOfAnotherEntityIsNotHandled() {
    Assertions.assertFalse(this.router.willHandleRequest(
        request("https://myentity.com/other-resolver/discovery?trust_anchor=https://myentity.com/ta"),
        record(null)));
  }

  @Test
  void otherEndpointsAreNotHandled() {
    Assertions.assertFalse(this.router.willHandleRequest(
        request(ENTITY_ID + "/.well-known/openid-federation"),
        record(null)));
  }

  @Test
  void missingResolverConfigurationGivesJsonNotFound() {
    final CompositeRecordSource source = Mockito.mock(CompositeRecordSource.class);
    Mockito.when(source.getResolverProperties()).thenReturn(List.of());
    final ServerResponseErrorHandler errorHandler = Mockito.mock(ServerResponseErrorHandler.class);
    Mockito.when(errorHandler.handle(Mockito.any()))
        .thenAnswer(invocation -> ServerResponse.status(invocation.getArgument(0, FederationException.class)
            .httpStatusCode()).build());
    final ResolverRouter withSource =
        new ResolverRouter(null, null, errorHandler, null, null, ObservationRegistry.NOOP, source);

    final CachedResponse response = withSource.handleRequest(
        request(ENTITY_ID + "/resolve?sub=https://myentity.com/rp&trust_anchor=https://myentity.com/ta"),
        record(null));

    Assertions.assertEquals(404, response.statusCode());
    Assertions.assertTrue(response.body().contains("not_found"), response.body());
  }

  @Test
  void explainReturnsTheExplanationInsteadOfResolving() throws FederationException {
    final ResolverProperties properties = new ResolverProperties(
        "https://myentity.com/ta", Duration.ofHours(1), null, ENTITY_ID);
    final CompositeRecordSource source = Mockito.mock(CompositeRecordSource.class);
    Mockito.when(source.getResolverProperties()).thenReturn(List.of(properties));
    final Resolver resolver = Mockito.mock(Resolver.class);
    Mockito.when(resolver.explain(Mockito.any()))
        .thenReturn(Map.of(0, Map.of("type", "Entity does not list superior in its authority_hints")));
    final ResolverFactory factory = Mockito.mock(ResolverFactory.class);
    Mockito.when(factory.create(properties)).thenReturn(resolver);
    final ResolverRouter withSource =
        new ResolverRouter(factory, null, null, null, null, ObservationRegistry.NOOP, source);

    final String uri = ENTITY_ID
        + "/resolve?sub=https://myentity.com/rp&trust_anchor=https://myentity.com/ta&explain=true";
    final ServerRequest request = request(uri);
    final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("sub", "https://myentity.com/rp");
    params.add("trust_anchor", "https://myentity.com/ta");
    params.add("explain", "true");
    Mockito.when(request.params()).thenReturn(params);

    final CachedResponse response = withSource.handleRequest(request, record(null));

    Assertions.assertEquals(200, response.statusCode());
    Assertions.assertEquals("application/json", response.contentType());
    Assertions.assertTrue(response.body().contains("authority_hints"), response.body());
    Mockito.verify(resolver, Mockito.never()).resolve(Mockito.any());
  }

  private static ServerRequest request(final String uri) {
    final ServerRequest request = Mockito.mock(ServerRequest.class);
    Mockito.when(request.uri()).thenReturn(URI.create(uri));
    Mockito.when(request.method()).thenReturn(HttpMethod.GET);
    return request;
  }

  private static EntityRecord record(final String virtualEntityId) {
    return EntityRecord.builder()
        .entityIdentifier(new EntityID(ENTITY_ID))
        .virtualEntityId(virtualEntityId != null ? new EntityID(virtualEntityId) : null)
        .metadata(Map.of("federation_entity", Map.of(
            "federation_resolve_endpoint", "/resolve"
        )))
        .build();
  }
}
