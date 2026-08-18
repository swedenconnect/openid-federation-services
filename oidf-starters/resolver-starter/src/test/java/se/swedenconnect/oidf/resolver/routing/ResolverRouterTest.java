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
import org.springframework.web.servlet.function.ServerRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.net.URI;
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

  private static ServerRequest request(final String uri) {
    final ServerRequest request = Mockito.mock(ServerRequest.class);
    Mockito.when(request.uri()).thenReturn(URI.create(uri));
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
