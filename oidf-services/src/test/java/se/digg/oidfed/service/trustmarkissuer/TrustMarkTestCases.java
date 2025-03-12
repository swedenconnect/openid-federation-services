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
package se.digg.oidfed.service.trustmarkissuer;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import se.digg.oidfed.service.entity.TestFederationEntities;
import se.digg.oidfed.service.router.responses.TrustMarkStatusReply;
import se.digg.oidfed.service.testclient.FederationClients;
import se.digg.oidfed.service.testclient.TestFederationClientParameterResolver;
import se.digg.oidfed.suites.Context;

import java.text.ParseException;
import java.util.List;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class TrustMarkTestCases {


  public static final EntityID TRUST_MARK_ID = new EntityID("https://authorization.local.swedenconnect.se/authorization-tmi/certified");

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
    // rest of setup.
    RestAssured.port = Context.getServicePort();
    RestAssured.basePath = "/tm";
  }

  @Test
  public void testTrustMark(final FederationClients clients) throws ParseException {
    final SignedJWT trustMark = clients.authorization().trustMark().trustMark(
        TestFederationEntities.Authorization.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.Authorization.OP_1
    );

    Assertions.assertEquals(TestFederationEntities.Authorization.TRUST_MARK_ISSUER.getValue(),
        trustMark.getJWTClaimsSet().getIssuer());
  }

  @Test
  public void testTrustMarkListing(final FederationClients clients) {
    final List<String> trustMarkListing = clients.authorization().trustMark().trustMarkListing(
        TestFederationEntities.Authorization.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        null
    );
    Assertions.assertFalse(trustMarkListing.isEmpty(), "Found 0 trust marks");
    final String expected = TestFederationEntities.Authorization.OP_1.getValue();
    Assertions.assertTrue(trustMarkListing.contains(expected), "Trust " +
        "Mark Listing does not contain expected value:%s".formatted(expected));
  }

  @Test
  public void testTrustMarkStatusActive(final FederationClients clients) {
    final TrustMarkStatusReply status = clients.authorization().trustMark()
        .trustMarkStatus(
            TestFederationEntities.Authorization.TRUST_MARK_ISSUER,
            TRUST_MARK_ID,
            TestFederationEntities.Authorization.OP_1,
            null
        );
    Assertions.assertTrue(status.getActive());
  }

  @Test
  public void testTrustMarkStatusNotActive(final FederationClients clients) throws InterruptedException {
    Thread.sleep(3000L);
    final TrustMarkStatusReply status = clients.authorization().trustMark().trustMarkStatus(
        TestFederationEntities.Authorization.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.Authorization.OP_2,
        null
    );
    Assertions.assertFalse(status.getActive());
  }


  @Test
  public void testTrustMarkStatusError(final FederationClients clients) {
    final Runnable throwingRunnable = () -> clients.authorization().trustMark().trustMarkStatus(
        TestFederationEntities.Authorization.TRUST_MARK_ISSUER,
        new EntityID("https://authorization.local.swedenconnect.se/not-found"),
        null,
        null
    );
    Assertions.assertThrows(RuntimeException.class, throwingRunnable::run);
  }

}
