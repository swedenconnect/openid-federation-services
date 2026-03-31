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
package se.swedenconnect.oidf.service;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.service.testclient.TrustAnchorClient;
import se.swedenconnect.oidf.service.suites.Context;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class CacheTestCases {

  public static final int TEST_END_EXCLUSIVE = 1000;
  public static final int NO_CACHE_TEST_END_EXCLUSIVE = 10;

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

  @Test
  @DisplayName("Subordinate fetch 1000 times with cache")
  void testFetchCaching(final FederationClients clients) {
    final TrustAnchorClient client = clients.anarchy().trustAnchor();
    final List<String> body = client.subordinateListing(new SubordinateListingRequest(null, null, null, null));
    final SignedJWT fetch = clients.anarchy().trustAnchor().fetch(new EntityID(body.getFirst()));

    IntStream.range(0, TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT fetchResponse = clients.anarchy().trustAnchor().fetch(new EntityID(body.getFirst()));
      Assertions.assertEquals(fetch.serialize(), fetchResponse.serialize());
    });
  }

  @Test
  @DisplayName("Subordinate fetch 10 times without cache")
  void testFetchNoCaching(final FederationClients clients) {
    final List<String> body =
        clients.disableCaching().anarchy().trustAnchor()
            .subordinateListing(new SubordinateListingRequest(null, null, null, null));
    final SignedJWT fetch = clients.disableCaching().anarchy().trustAnchor().fetch(new EntityID(body.getFirst()));

    IntStream.range(0, NO_CACHE_TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT fetchResponse = clients.disableCaching().anarchy().trustAnchor().fetch(new EntityID(body.getFirst()));
      Assertions.assertNotEquals(fetch.serialize(), fetchResponse.serialize());
    });
  }

  @Test
  @DisplayName("Trust mark 1000 times with cache")
  void testTrustMarkCaching(final FederationClients clients) {
    final SignedJWT trustMark = clients.trustMarkIssuerClient().trustMark().trustMark(
        new EntityID(TestFederationEntities.IM.TRUST_MARK_ISSUER.getValue()),
        new EntityID("http://localhost:11111/im/tmi/certified"),
        new EntityID("http://localhost:11111/im/op")
    );
    IntStream.range(0, TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT trustMarkResponse = clients.trustMarkIssuerClient().trustMark().trustMark(
          new EntityID(TestFederationEntities.IM.TRUST_MARK_ISSUER.getValue()),
          new EntityID("http://localhost:11111/im/tmi/certified"),
          new EntityID("http://localhost:11111/im/op")
      );
      Assertions.assertEquals(trustMark.serialize(), trustMarkResponse.serialize(),
          "Failed on iteration %d".formatted(i));
    });
  }

  @Test
  @DisplayName("Trust mark 10 times with no cache")
  void testTrustMarkNoCaching(final FederationClients clients) {
    final SignedJWT trustMark = clients.disableCaching().anarchy().trustMark().trustMark(
        new EntityID("http://localhost:11111/im/tmi"),
        new EntityID("http://localhost:11111/im/tmi/certified"),
        new EntityID("http://localhost:11111/im/op")
    );
    IntStream.range(0, NO_CACHE_TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT trustMarkResponse = clients.disableCaching().anarchy().trustMark().trustMark(
          new EntityID("http://localhost:11111/im/tmi"),
          new EntityID("http://localhost:11111/im/tmi/certified"),
          new EntityID("http://localhost:11111/im/op")
      );
      Assertions.assertNotEquals(trustMark.serialize(), trustMarkResponse.serialize());
    });
  }

  @Test
  @DisplayName("Entity Configuration 1000 times with cache")
  void entityConfigurationCache(final FederationClients clients) {
    final EntityStatement entityConfigurationReference =
        clients.entity().getEntityConfiguration(TestFederationEntities.IM.OP);

    IntStream.range(0, TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final EntityStatement entityConfigurationResponse =
          clients.entity().getEntityConfiguration(TestFederationEntities.IM.OP);
      Assertions.assertEquals(entityConfigurationReference.getSignedStatement().serialize()
          , entityConfigurationResponse.getSignedStatement().serialize());
    });
  }

  @Test
  @DisplayName("Entity Configuration 10 times with no cache")
  void entityConfigurationNoCache(final FederationClients clients) {
    final EntityStatement entityConfigurationReference =
        clients.disableCaching().entity().getEntityConfiguration(TestFederationEntities.IM.OP);

    IntStream.range(0, NO_CACHE_TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final EntityStatement entityConfigurationResponse =
          clients.disableCaching().entity().getEntityConfiguration(TestFederationEntities.IM.OP);
      Assertions.assertNotEquals(entityConfigurationReference.getSignedStatement().serialize()
          , entityConfigurationResponse.getSignedStatement().serialize());
    });
  }

  @Test
  @DisplayName("Resolve 1000 times with cache")
  void testResolveCaching(final FederationClients clients) {
    final SignedJWT reference = clients.anarchy().resolver().resolve(
        TestFederationEntities.IM.OP,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        null
    );
    IntStream.range(0, TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT response = clients.anarchy().resolver().resolve(
          TestFederationEntities.IM.OP,
          TestFederationEntities.Anarchy.TRUST_ANCHOR,
          null
      );
      Assertions.assertEquals(reference.serialize(), response.serialize(),
          "Failed on iteration %d".formatted(i));
    });
  }

  @Test
  @DisplayName("Resolve 10 times without cache")
  void testResolveNoCaching(final FederationClients clients) {
    final SignedJWT reference = clients.disableCaching().anarchy().resolver().resolve(
        TestFederationEntities.IM.OP,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        null
    );
    IntStream.range(0, NO_CACHE_TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final SignedJWT response = clients.disableCaching().anarchy().resolver().resolve(
          TestFederationEntities.IM.OP,
          TestFederationEntities.Anarchy.TRUST_ANCHOR,
          null
      );
      Assertions.assertNotEquals(reference.serialize(), response.serialize());
    });
  }

  @Test
  @DisplayName("Trust mark status 1000 times with cache")
  void testTrustMarkStatusCaching(final FederationClients clients) {
    final SignedJWT trustMark = clients.trustMarkIssuerClient().trustMark().trustMark(
        new EntityID(TestFederationEntities.IM.TRUST_MARK_ISSUER.getValue()),
        new EntityID("http://localhost:11111/im/tmi/certified"),
        new EntityID("http://localhost:11111/im/op")
    );
    final String statusReference = clients.trustMarkIssuerClient().trustMark().trustMarkStatus(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        trustMark.serialize()
    );
    IntStream.range(0, TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final String statusResponse = clients.trustMarkIssuerClient().trustMark().trustMarkStatus(
          TestFederationEntities.IM.TRUST_MARK_ISSUER,
          trustMark.serialize()
      );
      Assertions.assertEquals(statusReference, statusResponse,
          "Failed on iteration %d".formatted(i));
    });
  }

  @Test
  @DisplayName("Trust mark status 10 times without cache")
  void testTrustMarkStatusNoCaching(final FederationClients clients) {
    final SignedJWT trustMark = clients.disableCaching().trustMarkIssuerClient().trustMark().trustMark(
        new EntityID(TestFederationEntities.IM.TRUST_MARK_ISSUER.getValue()),
        new EntityID("http://localhost:11111/im/tmi/certified"),
        new EntityID("http://localhost:11111/im/op")
    );
    final String statusReference = clients.disableCaching().trustMarkIssuerClient().trustMark().trustMarkStatus(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        trustMark.serialize()
    );
    IntStream.range(0, NO_CACHE_TEST_END_EXCLUSIVE).parallel().forEach(i -> {
      final String statusResponse = clients.disableCaching().trustMarkIssuerClient().trustMark().trustMarkStatus(
          TestFederationEntities.IM.TRUST_MARK_ISSUER,
          trustMark.serialize()
      );
      Assertions.assertNotEquals(statusReference, statusResponse);
    });
  }
}
