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

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;

import java.text.ParseException;
import java.util.Map;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class ResolverSamlSpTestCases {

  @Test
  void testResolveSamlSp(final FederationClients clients) throws ParseException {
    final SignedJWT resolved = clients.anarchy().resolver().resolve(
        TestFederationEntities.IM.SP,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        null
    );

    final Map<String, Object> claims = resolved.getJWTClaimsSet().toJSONObject();
    Assertions.assertEquals(TestFederationEntities.IM.SP.getValue(), claims.get("sub"));

    final Map<String, Object> metadata = (Map<String, Object>) claims.get("metadata");
    Assertions.assertNotNull(metadata);
    Assertions.assertTrue(metadata.containsKey("saml_service_provider"),
        "Resolved metadata should contain saml_service_provider");

    final Map<String, Object> sp = (Map<String, Object>) metadata.get("saml_service_provider");
    Assertions.assertEquals("Example Organization", sp.get("organization_name"));
    Assertions.assertTrue((Boolean) sp.get("want_assertions_signed"));
    Assertions.assertTrue((Boolean) sp.get("authn_requests_signed"));

    final java.util.List<Map<String, Object>> trustMarks =
        (java.util.List<Map<String, Object>>) claims.get("trust_marks");
    Assertions.assertNotNull(trustMarks, "Resolved entity should carry a trust_marks claim");
    Assertions.assertTrue(trustMarks.isEmpty(),
        "SAML SP is not a registered subject for its trust-mark-source, "
            + "so trust marks fail to fetch during scrape, but resolution must still succeed");
  }

  @Test
  void testDiscoveryIncludesSamlSp(final FederationClients clients) {
    final var entities = clients.anarchy().resolver().discovery(
        new se.swedenconnect.oidf.resolver.DiscoveryRequest(
            TestFederationEntities.Anarchy.TRUST_ANCHOR.getValue(), null, null)
    );
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.SP.getValue()),
        "Discovery should include the SAML SP entity");
  }

  @Test
  void testDiscoveryFilteredBySamlSp(final FederationClients clients) {
    final var entities = clients.anarchy().resolver().discovery(
        new se.swedenconnect.oidf.resolver.DiscoveryRequest(
            TestFederationEntities.Anarchy.TRUST_ANCHOR.getValue(),
            java.util.List.of("saml_service_provider"),
            null)
    );
    Assertions.assertTrue(entities.contains(TestFederationEntities.IM.SP.getValue()),
        "Filtered discovery should include the SAML SP entity");
    Assertions.assertFalse(entities.contains(TestFederationEntities.IM.OP.getValue()),
        "Filtered discovery should exclude openid_provider entities");
  }
}
