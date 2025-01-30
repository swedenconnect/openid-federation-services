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
package se.digg.oidfed.service.resolver;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import se.digg.oidfed.service.IntegrationTestParent;
import se.digg.oidfed.service.entity.TestFederationEntities;
import se.digg.oidfed.service.testclient.FederationClients;

import java.text.ParseException;
import java.util.List;

class ResolverIT extends IntegrationTestParent {

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
    final Identifier trustMark = trustChain.getFirst().getClaimsSet().getTrustMarks().getFirst().getID();
    Assertions.assertEquals("https://authorization.local.swedenconnect.se/authorization-tmi/certified", trustMark.getValue());
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
        "Failed to validate trust chain:[se.digg.oidfed.resolver.chain.SignatureValidationStep]" +
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