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

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.common.exception.ServerErrorException;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing all operations towards TrustMark operations
 *
 * @author Per Fredrik Plars
 */
class TrustMarkIssuerTest {


  private TrustMarkIssuerProperties trustMarkIssuerProperties;

  private final RSAKey jwk;

  {
    try {
      jwk = new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .issueTime(new Date())
          .generate();
    } catch (JOSEException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  public void init() throws Exception {
    this.trustMarkIssuerProperties = TestDataSetup.trustMarkProperties();
  }

  @Test
  void trustMarkListing() throws NotFoundException, InvalidRequestException, JOSEException {

    final TrustMarkSubject sub1 =
        TrustMarkSubject.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkSubject sub2 =
        TrustMarkSubject.builder()
            .sub("http://sub2.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkSubject sub3 =
        TrustMarkSubject.builder()
            .sub("http://sub.outdated.se")
            .expires(Instant.now().plus(1, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    this.trustMarkIssuerProperties.trustMarks()
        .add(TrustMarkIssuerProperties.TrustMarkProperties.builder()
            .trustMarkId(TrustMarkId.create("http://tm1.digg.se"))
            .delegation(Optional.empty())
            .logoUri(Optional.empty())
            .refUri(Optional.empty())
            .build());

    final Clock fixed = Clock.fixed(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault());
    final InMemoryTrustMarkSubjectRepository subjectRepository = new InMemoryTrustMarkSubjectRepository(fixed);
    List.of(sub1, sub2, sub3).forEach(sub -> subjectRepository.register(new TrustMarkId("http://tm1.digg.se"), sub));
    final TrustMarkSigner signer = new TrustMarkSigner(new SignerFactory(new JWKSet(jwk))
        , fixed);
    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkIssuerProperties,
        signer
        , subjectRepository) {};
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

    final TrustMarkId trustMarkId = TrustMarkId.create("http://tm1.digg.se");
    this.trustMarkIssuerProperties.trustMarks().add(TrustMarkIssuerProperties.TrustMarkProperties.builder()
        .trustMarkId(trustMarkId)
        .refUri(Optional.of("http://digg.se/tm1/doc"))
        .logoUri(Optional.of("http://digg.se/tm1/logo.png"))
        .delegation(Optional.empty())
        .build());

    final TrustMarkSubject sub1 =
        TrustMarkSubject.builder()
            .sub("http://sub1.se")
            .granted(Instant.now())
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .build();



    final Clock fixed = Clock.fixed(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault());
    final InMemoryTrustMarkSubjectRepository subjectRepository = new InMemoryTrustMarkSubjectRepository(fixed);
    List.of(sub1).forEach(sub -> subjectRepository.register(trustMarkId, sub));
    final TrustMarkSigner signer = new TrustMarkSigner(new SignerFactory(new JWKSet(jwk))
        , fixed);

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkIssuerProperties, signer, subjectRepository);
    final String trustMarkJWT = trustMarkIssuer.trustMark(new TrustMarkRequest(trustMarkId.getTrustMarkId(),
        "http://sub1.se"));

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

    final TrustMarkSubject sub1 =
        TrustMarkSubject.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    final TrustMarkSubject expired =
        TrustMarkSubject.builder()
            .sub("http://expired.se")
            .expires(Instant.now().plus(6, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    this.trustMarkIssuerProperties.trustMarks().add(TrustMarkIssuerProperties.TrustMarkProperties.builder()
        .trustMarkId(TrustMarkId.create("http://tm1.digg.se"))
        .delegation(Optional.empty())
        .logoUri(Optional.empty())
        .refUri(Optional.empty())
        .build());

    final Clock fixed = Clock.fixed(Instant.now().plus(8, ChronoUnit.MINUTES), ZoneId.systemDefault());
    final InMemoryTrustMarkSubjectRepository subjectRepository = new InMemoryTrustMarkSubjectRepository(fixed);
    List.of(sub1, expired).forEach(sub -> subjectRepository.register(new TrustMarkId("http://tm1.digg.se"), sub));
    final TrustMarkSigner signer = new TrustMarkSigner(new SignerFactory(new JWKSet(jwk))
        , fixed);

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(trustMarkIssuerProperties, signer, subjectRepository);

    assertTrue(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se",
            "http://sub1.se", null)));
    assertFalse(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se",
            "http://expired.se", null)));
  }

  @Test
  void testExpCalculationExpectTTLForSubject() throws InvalidRequestException {
    final Clock fixed = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final TrustMarkSigner signer = new TrustMarkSigner(new SignerFactory(new JWKSet(jwk))
        , fixed);
    final Instant ttlForSubject = Instant.now().plus(2, ChronoUnit.MINUTES);
    final Date exp = signer.calculateExp(Duration.ofMinutes(5), ttlForSubject);

    assertEquals(new Date(ttlForSubject.toEpochMilli()).toInstant(), exp.toInstant(),
        "Expected ttlForSubjectToBeSelected");
  }

  @Test
  void testExpCalculationExpectDuration() {
    final Instant now = Instant.now();
    final Clock fixed = Clock.fixed(now, ZoneId.systemDefault());
    final TrustMarkSigner signer = new TrustMarkSigner(new SignerFactory(new JWKSet(jwk))
        , fixed);
    final Instant ttlForSubject = now.plus(10, ChronoUnit.MINUTES);
    final Duration durationTTL = Duration.ofMinutes(5);
    final Date exp = signer.calculateExp(durationTTL, ttlForSubject);

    assertEquals(new Date(now.plus(durationTTL).toEpochMilli()).toInstant(), exp.toInstant(),
        "Expected duration to be selected");
  }
}