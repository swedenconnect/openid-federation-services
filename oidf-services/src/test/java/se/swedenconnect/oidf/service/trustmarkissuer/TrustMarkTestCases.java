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
package se.swedenconnect.oidf.service.trustmarkissuer;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.router.responses.TrustMarkStatusReply;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.suites.Context;

import java.text.ParseException;
import java.util.List;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class TrustMarkTestCases {


  public static final EntityID TRUST_MARK_ID = new EntityID("http://localhost:11111/im/tmi/certified");

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
    final SignedJWT trustMark = clients.anarchy().trustMark().trustMark(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.IM.OP
    );

    Assertions.assertEquals(TestFederationEntities.IM.TRUST_MARK_ISSUER.getValue(),
        trustMark.getJWTClaimsSet().getIssuer());
  }

  @Test
  public void testTrustMarkListing(final FederationClients clients) {
    final List<String> trustMarkListing = clients.anarchy().trustMark().trustMarkListing(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        null
    );
    Assertions.assertFalse(trustMarkListing.isEmpty(), "Found 0 trust marks");
    final String expected = TestFederationEntities.IM.OP.getValue();
    Assertions.assertTrue(trustMarkListing.contains(expected), "Trust " +
        "Mark Listing does not contain expected value:%s".formatted(expected));
  }

  @Test
  public void testTrustMarkStatusActive(final FederationClients clients) {
    final TrustMarkStatusReply status = clients.anarchy().trustMark()
        .trustMarkStatus(
            TestFederationEntities.IM.TRUST_MARK_ISSUER,
            TRUST_MARK_ID,
            TestFederationEntities.IM.OP,
            null
        );
    Assertions.assertTrue(status.getActive());
  }

  @Test
  public void testTrustMarkStatusNotActive(final FederationClients clients) throws InterruptedException {
    Thread.sleep(3000L);
    final TrustMarkStatusReply status = clients.anarchy().trustMark().trustMarkStatus(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.IM.NestedIM.OP,
        null
    );
    Assertions.assertFalse(status.getActive());
  }


  @Test
  public void testTrustMarkStatusError(final FederationClients clients) {
    final Runnable throwingRunnable = () -> clients.anarchy().trustMark().trustMarkStatus(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        new EntityID("http://localhost:11111/im/im/accepted"),
        null,
        null
    );
    Assertions.assertThrows(RuntimeException.class, throwingRunnable::run);
  }

}
