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
package se.swedenconnect.oidf.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.jwt.JWKSetSignerFactory;

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

    final CompositeRecordSource source = Mockito.mock(CompositeRecordSource.class);

    final TrustMarkSubjectProperty sub1 =
        TrustMarkSubjectProperty.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkSubjectProperty sub2 =
        TrustMarkSubjectProperty.builder()
            .sub("http://sub2.se")
            .expires(Instant.now().plus(10, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    final TrustMarkSubjectProperty sub3 =
        TrustMarkSubjectProperty.builder()
            .sub("http://sub.outdated.se")
            .expires(Instant.now().minus(1, ChronoUnit.DAYS))
            .granted(Instant.now())
            .build();

    Mockito.when(source.getTrustMarkSubjects(trustMarkIssuerProperties.entityIdentifier(),
            TrustMarkType.create("http://tm1.digg.se")))
        .thenReturn(List.of(sub1, sub2, sub3));


    this.trustMarkIssuerProperties.trustMarks()
        .add(TrustMarkProperties.builder()
            .trustMarkType(TrustMarkType.create("http://tm1.digg.se"))
            .delegation(null)
            .logoUri(null)
            .refUri(null)
            .build());

    final Clock fixed = Clock.fixed(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault());


    final TrustMarkSigner signer = new TrustMarkSigner(new JWKSetSignerFactory()
        , fixed);
    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(this.trustMarkIssuerProperties, signer, source, Clock.systemUTC());
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
  public void testTrustMarkValidity()
      throws NotFoundException, InvalidRequestException {

    final CompositeRecordSource source = Mockito.mock(CompositeRecordSource.class);

    final TrustMarkSubjectProperty sub1 =
        TrustMarkSubjectProperty.builder()
            .sub("http://sub1.se")
            .expires(Instant.now().plus(10, ChronoUnit.MINUTES))
            .granted(Instant.now())
            .build();

    final TrustMarkSubjectProperty expired =
        TrustMarkSubjectProperty.builder()
            .sub("http://expired.se")
            .expires(Instant.now().minus(6, ChronoUnit.MINUTES))
            .granted(Instant.now().minus(10, ChronoUnit.MINUTES))
            .build();

    this.trustMarkIssuerProperties.trustMarks().add(TrustMarkProperties.builder()
        .trustMarkType(TrustMarkType.create("http://tm1.digg.se"))
        .delegation(null)
        .logoUri(null)
        .refUri(null)
        .build());

    Mockito.when(source.getTrustMarkSubject(
        this.trustMarkIssuerProperties.entityIdentifier(),
        TrustMarkType.create("http://tm1.digg.se"),
        new EntityID(sub1.sub())
    )).thenReturn(Optional.of(sub1));


    final Clock fixed = Clock.fixed(Instant.now().plus(8, ChronoUnit.MINUTES), ZoneId.systemDefault());
    final TrustMarkSigner signer = new TrustMarkSigner(new JWKSetSignerFactory()
        , fixed);

    final TrustMarkIssuer trustMarkIssuer = new TrustMarkIssuer(trustMarkIssuerProperties, signer, source, Clock.systemUTC());

    assertTrue(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se",
            "http://sub1.se", null)));


    Mockito.when(source.getTrustMarkSubject(
        this.trustMarkIssuerProperties.entityIdentifier(),
        TrustMarkType.create("http://tm1.digg.se"),
        new EntityID(expired.sub())
    )).thenReturn(Optional.of(expired));

    assertFalse(
        trustMarkIssuer.trustMarkStatus(new TrustMarkStatusRequest("http://tm1.digg.se",
            "http://expired.se", null)));
  }

  @Test
  void testExpCalculationExpectTTLForSubject() throws InvalidRequestException {
    final Clock fixed = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final TrustMarkSigner signer = new TrustMarkSigner(new JWKSetSignerFactory()
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
    final TrustMarkSigner signer = new TrustMarkSigner(new JWKSetSignerFactory()
        , fixed);
    final Instant ttlForSubject = now.plus(10, ChronoUnit.MINUTES);
    final Duration durationTTL = Duration.ofMinutes(5);
    final Date exp = signer.calculateExp(durationTTL, ttlForSubject);

    assertEquals(new Date(now.plus(durationTTL).toEpochMilli()).toInstant(), exp.toInstant(),
        "Expected duration to be selected");
  }
}