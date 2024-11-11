/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.configuration.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.exception.InvalidRequestException;
import se.digg.oidfed.trustmarkissuer.exception.NotFoundException;
import se.digg.oidfed.trustmarkissuer.exception.ServerErrorException;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing all operations towards TrustMark operations
 *
 * @author Per Fredrik Plars
 */
class TrustMarkIssuerTest {

  private TrustMarkProperties trustMarkProperties;

  @BeforeEach
  public void init() throws Exception {
    this.trustMarkProperties = TestDataSetup.trustMarkProperties();
  }

  @Test
  void trustMarkListing() throws NotFoundException, InvalidRequestException {

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub1 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub2 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://sub2.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub3 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://sub.outdated.se")
            .expires(Instant.now().plus(1, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    this.trustMarkProperties.getTrustMarks()
        .add(TrustMarkIssuerProperties.builder()
            .trustMarkId(TrustMarkId.create("http://tm1.digg.se"))
            .subjects(List.of(sub1, sub2, sub3))
            .build());

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties) {
      @Override
      protected Instant now() {
        return Instant.now().plus(2, ChronoUnit.DAYS);
      }
    };
    assertThrows(InvalidRequestException.class, () -> trustMarkIssuer.trustMarkListing(null));
    assertThrows(InvalidRequestException.class,
        () -> trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(null, null)));
    assertThrows(NotFoundException.class,
        () -> trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest("http://www.i.trust.u.se", null)));

    final List<String> search1 =
        trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest("http://tm1.digg.se", null));
    assertEquals(2, search1.size());

    final List<String> search2 = trustMarkIssuer.trustMarkListing(
        new TrustMarkListingRequest("http://tm1.digg.se", "http://sub.notfound"));
    assertEquals(0, search2.size());

    final List<String> search3 =
        trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest("http://tm1.digg.se", "http://sub1.se"));
    assertEquals(1, search3.size());
    assertEquals("http://sub1.se", search3.getFirst());

    final List<String> search4 = trustMarkIssuer.trustMarkListing(
        new TrustMarkListingRequest("http://tm1.digg.se", "http://sub.outdated.se"));
    assertEquals(0, search4.size());

  }

  @Test
  public void testTrustMarkCreation()
      throws NotFoundException, InvalidRequestException, ServerErrorException, ParseException {

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub1 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    this.trustMarkProperties.getTrustMarks().add(TrustMarkIssuerProperties.builder()
        .trustMarkId(TrustMarkId.create("http://tm1.digg.se"))
            .refUri("http://digg.se/tm1/doc")
            .logoUri("http://digg.se/tm1/logo.png")
        .subjects(List.of(sub1))
        .build());

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties);
    final String trustMarkJWT = trustMarkIssuer.trustMark(new TrustMarkRequest("http://tm1.digg.se", "http://sub1.se"));

    final SignedJWT tm = SignedJWT.parse(trustMarkJWT);
    final JWTClaimsSet unpackedClaims = tm.getJWTClaimsSet();

    assertNotNull(unpackedClaims.getIssuer());
    assertNotNull(unpackedClaims.getExpirationTime());
    assertNotNull(unpackedClaims.getClaim("ref"));
    assertNotNull(unpackedClaims.getClaim("logo_uri"));
    assertNotNull(unpackedClaims.getClaim("iss"));
    assertNotNull(unpackedClaims.getClaim("id"));
    assertNotNull(unpackedClaims.getClaim("exp"));
    assertNotNull(unpackedClaims.getClaim("iat"));
    assertNotNull(unpackedClaims.getClaim("jti"));

    final JWSHeader header = tm.getHeader();
    assertNotNull(header.getAlgorithm());
    assertNotNull(header.getType());
    assertNotNull(header.getKeyID());

    System.out.println(trustMarkJWT);
    System.out.println(unpackedClaims);
  }

  @Test
  public void testTrustMarkValidity()
      throws NotFoundException, InvalidRequestException {

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties sub1 =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    final TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties expired =
        TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties.builder()
            .sub("http://expired.se")
            .expires(Instant.now().plus(6, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    this.trustMarkProperties.getTrustMarks().add(TrustMarkIssuerProperties.builder()
        .trustMarkId(TrustMarkId.create("http://tm1.digg.se"))
        .subjects(List.of(sub1, expired))
        .build());

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties) {
      @Override
      protected Instant now() {
        return Instant.now().plus(8, ChronoUnit.MINUTES);
      }
    };

    assertTrue(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se", "http://sub1.se", null)));
    assertFalse(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se", "http://expired.se", null)));
  }

  @Test
  void testExpCalculationExpectTTLForSubject() throws InvalidRequestException {

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties);
    final Instant ttlForSubject = Instant.now().plus(2, ChronoUnit.MINUTES);
    final Date exp = trustMarkIssuer.calculateExp(Duration.ofMinutes(5), ttlForSubject);

    assertEquals(new Date(ttlForSubject.toEpochMilli()).toInstant(), exp.toInstant(),
        "Expected ttlForSubjectToBeSelected");
  }

  @Test
  void testExpCalculationExpectDuration() {

    final Instant now = Instant.now();
    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties) {
      @Override
      protected Instant now() {
        return now;
      }
    };
    final Instant ttlForSubject = Instant.now().plus(10, ChronoUnit.MINUTES);
    final Duration durationTTL = Duration.ofMinutes(5);
    final Date exp = trustMarkIssuer.calculateExp(durationTTL, ttlForSubject);

    assertEquals(new Date(now.plus(durationTTL).toEpochMilli()).toInstant(), exp.toInstant(),
        "Expected duration to be selected");
  }

  @Test
  void testValidityInTime() throws NotFoundException, InvalidRequestException {
    final Instant now = Instant.now();
    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkProperties) {
      @Override
      protected Instant now() {
        return now;
      }
    };

    assertTrue(trustMarkIssuer.isTrustMarkValidInTime(TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties
        .builder()
        .granted(Instant.now().minus(10, ChronoUnit.MINUTES))
        .expires(Instant.now().plus(30, ChronoUnit.MINUTES))
        .build()));

    assertFalse(trustMarkIssuer.isTrustMarkValidInTime(TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties
        .builder()
        .revoked(true)
        .build()));

    assertFalse(trustMarkIssuer.isTrustMarkValidInTime(TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties
        .builder()
        .granted(Instant.now().plus(10, ChronoUnit.MINUTES))
        .expires(Instant.now().plus(30, ChronoUnit.MINUTES))
        .build()));

  }

}