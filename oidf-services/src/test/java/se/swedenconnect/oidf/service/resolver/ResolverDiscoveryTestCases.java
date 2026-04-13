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
package se.swedenconnect.oidf.service.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.ResolverClient;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;

import java.util.List;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class ResolverDiscoveryTestCases {

  @Test
  void testDiscoveryReturnsEntities(final FederationClients clients) {
    final ResolverClient resolver = clients.anarchy().resolver();
    final List<String> entities = resolver.discovery(
        new DiscoveryRequest(TestFederationEntities.Anarchy.TRUST_ANCHOR.getValue(), null, null)
    );
    Assertions.assertFalse(entities.isEmpty());
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.OP.getValue()));
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.NestedIM.OP.getValue()));
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.NestedIM.INTERMEDIATE.getValue()));
  }

  @Test
  void testDiscoveryFilteredByEntityTypeOpenidProvider(final FederationClients clients) {
    final ResolverClient resolver = clients.anarchy().resolver();
    final List<String> entities = resolver.discovery(
        new DiscoveryRequest(TestFederationEntities.Anarchy.TRUST_ANCHOR.getValue(), List.of("openid_provider"), null)
    );
    Assertions.assertFalse(entities.isEmpty());
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.OP.getValue()));
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.NestedIM.OP.getValue()));
    Assertions.assertFalse(entities.contains(TestFederationEntities.IM.NestedIM.INTERMEDIATE.getValue()));
  }

  @Test
  void testDiscoveryFilteredByEntityTypeRelyingParty(final FederationClients clients) {
    final ResolverClient resolver = clients.anarchy().resolver();
    final List<String> entities = resolver.discovery(
        new DiscoveryRequest(TestFederationEntities.Anarchy.TRUST_ANCHOR.getValue(), List.of("relying_party"), null)
    );
    Assertions.assertFalse(entities.isEmpty());
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.NestedIM.RP.getValue()));
    Assertions.assertFalse(entities.contains(TestFederationEntities.IM.OP.getValue()));
    Assertions.assertFalse(entities.contains(TestFederationEntities.IM.NestedIM.OP.getValue()));
  }
}
