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
package se.swedenconnect.oidf.service.trustanchor;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.HttpClientErrorException;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.keys.FederationKeys;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.service.testclient.TrustAnchorClient;
import se.swedenconnect.oidf.service.suites.Context;

import java.util.List;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class TrustAnchorTestCases {

  public static final String TRUST_MARK_ID = "https://authorization.local.swedenconnect.se/authorization-tmi/certified";

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

  @Test
  @DisplayName("Subordinate listing : 200")
  void testListSubordinatesAndFetchEntityStatement(final FederationClients clients) {
    final TrustAnchorClient client = clients.authorization().trustAnchor();
    final List<?> body = client.subordinateListing(new SubordinateListingRequest(null, null, null, null));
    Assertions.assertNotNull(body);
    Assertions.assertFalse(body.isEmpty());
  }

  @Test
  @DisplayName("Fetch existing subject : 200")
  void testFetchEntityStatement(final FederationClients clients) throws JOSEException {
    final TrustAnchorClient client = clients.authorization().trustAnchor();
    final SignedJWT signedJWT = client.fetch(TestFederationEntities.Authorization.OP_1);
    Assertions.assertNotNull(signedJWT);
    final FederationKeys keys = Context.applicationContext.get().getBean(FederationKeys.class);
    final JWK first = keys.signKeys().toPublicJWKSet().toPublicJWKSet().getKeys().getFirst();
    final JWSVerifier jwsVerifier = new DefaultJWSVerifierFactory().createJWSVerifier(signedJWT.getHeader(), first.toRSAKey().toKeyPair().getPublic());
    signedJWT.verify(jwsVerifier);
  }

  @Test
  @DisplayName("Fetch non-existing subject : 404")
  void fetchFailsWhenInvalidEntityId(final FederationClients clients) {
    final TrustAnchorClient client = clients.authorization().trustAnchor();
    final Runnable throws404 = () -> client.fetch(new EntityID("https://authorization.local.swedenconnect.se/op-3"));

    Assertions.assertThrows(HttpClientErrorException.NotFound.class, throws404::run);
  }

  @Test
  @DisplayName("Fecth missing parameter subject : 400")
  void fetchFailsWhenMandatoryParameterIsMissing(final FederationClients clients) {
    final TrustAnchorClient client = clients.authorization().trustAnchor();
    final Runnable throws400 = () -> client.fetch(null);

    Assertions.assertThrows(HttpClientErrorException.BadRequest.class, throws400::run);
  }

  @ParameterizedTest(name = "{index} 200: {0}")
  @MethodSource("se.swedenconnect.oidf.service.trustanchor.TestParameters#okSubordinateListingParameters")
  void subordinateListing(final SubordinateListingRequest request, final List<EntityID> expectedEntries,
                          final FederationClients clients) {
    final List<String> body = clients.authorization().trustAnchor().subordinateListing(request);
    Assertions.assertNotNull(body);
    Assertions.assertEquals(expectedEntries.size(), body.size(),
        "Size did not match expected entries expected:%s actual:%s".formatted(expectedEntries, body));

    Assertions.assertTrue(body.containsAll(expectedEntries.stream().map(Identifier::getValue).toList()));
  }

  @ParameterizedTest(name = "{index} Error: {0} {1}")
  @MethodSource("se.swedenconnect.oidf.service.trustanchor.TestParameters#nokSubordinateListingParameters")
  void subordinateListingErrors(final SubordinateListingRequest request, final Class<? extends Exception> expected,
                                final FederationClients clients) {
    Runnable expectThrows = () -> clients.authorization().trustAnchor().subordinateListing(request);
    Assertions.assertThrows(expected, expectThrows::run);
  }

}