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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.suites.Context;

import java.text.ParseException;
import java.util.List;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class ResolverTestCases {

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    Assumptions.assumeTrue(context);
  }


  @Test
  @DisplayName("Discovery OP : 200")
  void discoverEntitiesByType(final FederationClients clients) {
    final List<String> discovery = clients.municipality().resolver().discovery(new DiscoveryRequest(
        TestFederationEntities.Municipality.TRUST_ANCHOR.getValue(),
        List.of(EntityType.OPENID_PROVIDER.getValue()),
        null)
    );
    Assertions.assertEquals(2, discovery.size(), "Size of discovery was wrong %s".formatted(discovery));
  }

  @Test
  @DisplayName("Discovery ALL : 200")
  void discoverAllEntities(final FederationClients clients) {
    final List<String> discovery = clients.municipality().resolver().discovery(new DiscoveryRequest(
        TestFederationEntities.Municipality.TRUST_ANCHOR.getValue(),
        null,
        null)
    );
    Assertions.assertEquals(7, discovery.size(), "Size of discovery was wrong %s".formatted(discovery));
  }

  @Test
  @DisplayName("Discovery ALL : 200")
  void discoverByTrustMark(final FederationClients clients) {
    final List<String> discovery = clients.municipality().resolver().discovery(new DiscoveryRequest(
        TestFederationEntities.Municipality.TRUST_ANCHOR.getValue(),
        null,
        List.of(TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue() + "/certified"))
    );
    Assertions.assertEquals(1, discovery.size(), "Size of discovery was wrong %s".formatted(discovery));
  }

  @Test
  @DisplayName("Resolve Entity: 200")
  void resolveFederation(final FederationClients clients) throws ParseException {
    final SignedJWT resolve = clients.municipality().resolver()
        .resolve(
            TestFederationEntities.Authorization.OP_1,
            TestFederationEntities.Municipality.TRUST_ANCHOR,
            null
        );
    Assertions.assertNotNull(resolve);
    final List<EntityStatement> trustChain = resolve.getJWTClaimsSet().getStringListClaim("trust_chain")
        .stream()
        .map(es -> {
          try {
            return EntityStatement.parse(es);
          } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new RuntimeException(e);
          }
        }).toList();

    verifyTrustChain(trustChain);
    final Identifier trustMark = trustChain.getFirst().getClaimsSet().getTrustMarks().getFirst().getID();
    Assertions.assertEquals("https://authorization.local.swedenconnect.se/authorization-tmi/certified", trustMark.getValue());
  }

  @Test
  @DisplayName("Resolve Type TrustAnchor: 200")
  void resolveByType(final FederationClients clients) throws ParseException {
    final SignedJWT resolve = clients.municipality().resolver()
        .resolve(
            TestFederationEntities.Authorization.OP_1,
            TestFederationEntities.Municipality.TRUST_ANCHOR,
            EntityType.OPENID_PROVIDER
        );
    Assertions.assertNotNull(resolve);
    final List<EntityStatement> trustChain = resolve.getJWTClaimsSet().getStringListClaim("trust_chain")
        .stream()
        .map(es -> {
          try {
            return EntityStatement.parse(es);
          } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new RuntimeException(e);
          }
        }).toList();

    verifyTrustChain(trustChain);
    final String expectedTrustMark = "https://authorization.local.swedenconnect.se/authorization-tmi/certified";
    final String unexpectedTrustMark = "https://authorization.local.swedenconnect.se/authorization-tmi/uncertified";

    final List<TrustMarkEntry> trustMarks = trustChain.getFirst().getClaimsSet().getTrustMarks();

    final List<String> resolverTrustMarkIds =
        resolve.getJWTClaimsSet().getStringListClaim("trust_marks").stream().map(tm -> {
          try {
            return SignedJWT.parse(tm).getJWTClaimsSet().getStringClaim("trust_mark_id");
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }).toList();
    Assertions.assertEquals(1, resolverTrustMarkIds.size());
    Assertions.assertTrue(resolverTrustMarkIds.contains(expectedTrustMark));
    //Verify we filtered out invalid trust mark with resolver even though they remain in the entity configuration
    Assertions.assertEquals(2, trustMarks.size());
    Assertions.assertTrue(trustMarks.stream().anyMatch(tm -> tm.getID().getValue().equals(expectedTrustMark)));
    Assertions.assertTrue(trustMarks.stream().anyMatch(tm -> tm.getID().getValue().equals(unexpectedTrustMark)));
  }

  @Test
  @DisplayName("Resolve N/A subject: 404")
  void respondsWith404WhenEntityIsMissing(final FederationClients clients) {
    final Runnable shouldThrow404 = () -> {
      clients.municipality().resolver()
          .resolve(
              new EntityID("https://nosuchclient.test"),
              TestFederationEntities.Municipality.TRUST_ANCHOR,
              EntityType.OPENID_PROVIDER
          );
    };

    final HttpClientErrorException.NotFound exception = Assertions.assertThrows(HttpClientErrorException.NotFound.class, shouldThrow404::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("not_found", problem.getProperties().get("error"));
  }

  @Test
  @DisplayName("Resolve Wrong Entity Type: 404")
  void respondsWith404WhenEntityTypeDoesNotMatch(final FederationClients clients) {
    final Runnable shouldThrow404 = () -> {
      clients.municipality().resolver()
          .resolve(
              TestFederationEntities.Authorization.OP_1,
              TestFederationEntities.Municipality.TRUST_ANCHOR,
              EntityType.OPENID_RELYING_PARTY
          );
    };

    final HttpClientErrorException.NotFound exception = Assertions.assertThrows(HttpClientErrorException.NotFound.class, shouldThrow404::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("not_found", problem.getProperties().get("error"));
  }

  @Test
  @DisplayName("Resolve Missing Subject: 400")
  void respondsWith400WhenRequiredParameterSubjectIsMissing(final FederationClients clients) {
    final Runnable shouldThrow400 = () -> {
      clients.municipality().resolver()
          .resolve(
              null,
              TestFederationEntities.Municipality.TRUST_ANCHOR,
              EntityType.OPENID_RELYING_PARTY
          );
    };

    final HttpClientErrorException.BadRequest exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, shouldThrow400::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("invalid_request", problem.getProperties().get("error"));
    Assertions.assertEquals("Required request parameter [sub] was missing.", problem.getProperties().get(
        "error_description"));
  }

  @Test
  @DisplayName("Resolve Missing Trust Anchor: 400")
  void respondsWith400WhenRequiredParameterTrustAnchorIsMissing(final FederationClients clients) {
    final Runnable shouldThrow400 = () -> {
      clients.municipality().resolver()
          .resolve(
              TestFederationEntities.Authorization.OP_1,
              null,
              EntityType.OPENID_RELYING_PARTY
          );
    };

    final HttpClientErrorException.BadRequest exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, shouldThrow400::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("invalid_request", problem.getProperties().get("error"));
    Assertions.assertEquals("Required request parameter [trust_anchor] was missing.", problem.getProperties().get(
        "error_description"));
  }

  @Test
  @DisplayName("Resolve Wrong Trust Anchor: 400")
  void respondsWith404WhenRequiredParameterTrustAnchorWrong(final FederationClients clients) {
    final Runnable shouldThrow404 = () -> {
      clients.municipality().resolver()
          .resolve(
              TestFederationEntities.Authorization.OP_1,
              TestFederationEntities.PrivateSector.TRUST_ANCHOR,
              EntityType.OPENID_RELYING_PARTY
          );
    };

    final HttpClientErrorException.NotFound exception = Assertions.assertThrows(HttpClientErrorException.NotFound.class,
        shouldThrow404::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("invalid_trust_anchor", problem.getProperties().get("error"));
    Assertions.assertEquals("The Trust Anchor cannot be found or used.", problem.getProperties().get(
        "error_description"));
  }

  @Test
  @DisplayName("Resolve Broken Trust Chain: 400")
  void brokenTrustChain400(final FederationClients clients) {
    final Runnable shouldThrow400 = () -> {
      clients.municipality().resolver()
          .resolve(
              new EntityID("https://municipality.local.swedenconnect.se/misconfigured"),
              TestFederationEntities.Municipality.TRUST_ANCHOR,
              null
          );
    };

    final HttpClientErrorException.BadRequest exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, shouldThrow400::run);
    final ProblemDetail problem = exception.getResponseBodyAs(ProblemDetail.class);
    Assertions.assertEquals("invalid_trust_chain", problem.getProperties().get("error"));
    Assertions.assertEquals(
        "Failed to validate trust chain:[se.swedenconnect.oidf.resolver.chain.SignatureValidationStep]" +
            " messages:[Failed to validate trustchain signatures]"
        , problem.getProperties().get(
            "error_description"));
  }

  private static void verifyTrustChain(final List<EntityStatement> trustChain) {
    Assertions.assertDoesNotThrow(() -> trustChain.getFirst().verifySignatureOfSelfStatement());
    Assertions.assertDoesNotThrow(() -> trustChain.getLast().verifySignatureOfSelfStatement());
    Assertions.assertEquals(4, trustChain.size());

    for (int x = 1; x < trustChain.size() - 1; x++) {
      final int index = x;
      Assertions.assertDoesNotThrow(() -> trustChain.get(index).verifySignature(trustChain.get(index - 1).getClaimsSet().getJWKSet()));
      Assertions.assertEquals(
          trustChain.get(index).getClaimsSet().getSubject().getValue(),
          trustChain.get(index - 1).getClaimsSet().getIssuer().getValue()
      );
    }
  }
}