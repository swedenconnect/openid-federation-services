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

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
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
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.suites.Context;
import se.swedenconnect.oidf.service.time.TestClock;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  public void testTrustMarkStatusActive(final FederationClients clients) throws ParseException {

    final SignedJWT trustMark = clients.anarchy().trustMark().trustMark(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.IM.OP
    );

    final String statusJwt = clients.anarchy().trustMark()
        .trustMarkStatus(
            TestFederationEntities.IM.TRUST_MARK_ISSUER,
            trustMark.serialize()
        );
    Assertions.assertEquals("active", SignedJWT.parse(statusJwt).getJWTClaimsSet().getStringClaim("status"));
  }

  @Test
  public void testTrustMarkStatusInvalid(final FederationClients clients) throws ParseException {

    final SignedJWT trustMark = clients.anarchy().trustMark().trustMark(
        TestFederationEntities.IM.TRUST_MARK_ISSUER,
        TRUST_MARK_ID,
        TestFederationEntities.IM.OP
    );

    final SignedJWT parse = SignedJWT.parse(trustMark.serialize());
    final Base64URL header = parse.getHeader().toBase64URL();
    final Base64URL signature = parse.getSignature();
    final Base64URL payload = new JWTClaimsSet.Builder(parse.getJWTClaimsSet())
        .claim("iss", "https://me.test")
        .build().toPayload()
        .toBase64URL();
    final SignedJWT modifiedJwt = new SignedJWT(header, payload, signature);
    String statusJwt = clients.anarchy().trustMark()
        .trustMarkStatus(
            TestFederationEntities.IM.TRUST_MARK_ISSUER,
            modifiedJwt.serialize()
        );
    Assertions.assertEquals("invalid", SignedJWT.parse(statusJwt).getJWTClaimsSet().getStringClaim("status"));
  }

  @Test
  public void testTrustMarkStatusExpired(final FederationClients clients) throws InterruptedException, ParseException {
    final TestClock clock = Context.applicationContext.get().getBean(TestClock.class);
    try {
      clock.stopTime(Instant.now().minus(14, ChronoUnit.DAYS));
      final SignedJWT trustMark = clients.anarchy().trustMark().trustMark(
          TestFederationEntities.IM.TRUST_MARK_ISSUER,
          TRUST_MARK_ID,
          TestFederationEntities.IM.OP
      );
      clock.resumseTime();

      final String status = clients.anarchy().trustMark().trustMarkStatus(
          TestFederationEntities.IM.TRUST_MARK_ISSUER,
          trustMark.serialize()
      );
      Assertions.assertEquals("expired", SignedJWT.parse(status).getJWTClaimsSet().getStringClaim("status"));
    } finally {
      clock.resumseTime();
    }
  }
}
