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
package se.swedenconnect.oidf.trustmarkissuer;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.exception.ServerErrorException;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link TrustMarkIssuer#trustMark(TrustMarkRequest)} and
 * {@link TrustMarkIssuer#trustMarkListing(TrustMarkListingRequest)}.
 *
 * @author Martin Lindström
 */
class TrustMarkIssuerTrustMarkTest {

  private static final String TYPE = "http://tm.digg.se/default";
  private static final String SUBJECT = "http://tm1.digg.se/sub1";

  private TrustMarkSigner signer;
  private CompositeRecordSource source;

  @BeforeEach
  void setUp() {
    this.signer = Mockito.mock(TrustMarkSigner.class);
    this.source = Mockito.mock(CompositeRecordSource.class);
    Mockito.when(this.source.getEntity(Mockito.any())).thenReturn(Optional.of(Mockito.mock(EntityRecord.class)));
  }

  @Test
  void revokedSubjectIsNotFound() {
    final TrustMarkIssuer issuer = this.issuer(new TrustMarkSubjectProperty(SUBJECT, null, null, true));
    Assertions.assertThrows(NotFoundException.class, () -> issuer.trustMark(new TrustMarkRequest(TYPE, SUBJECT)));
  }

  @Test
  void expiredSubjectIsNotFound() {
    final TrustMarkIssuer issuer = this.issuer(
        new TrustMarkSubjectProperty(SUBJECT, null, Instant.now().minusSeconds(60), false));
    Assertions.assertThrows(NotFoundException.class, () -> issuer.trustMark(new TrustMarkRequest(TYPE, SUBJECT)));
  }

  @Test
  void notYetGrantedSubjectIsNotFound() {
    final TrustMarkIssuer issuer = this.issuer(
        new TrustMarkSubjectProperty(SUBJECT, Instant.now().plusSeconds(60), null, false));
    Assertions.assertThrows(NotFoundException.class, () -> issuer.trustMark(new TrustMarkRequest(TYPE, SUBJECT)));
  }

  @Test
  void validSubjectIsSigned() throws Exception {
    final SignedJWT trustMark = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder().build());
    trustMark.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
    Mockito.when(this.signer.sign(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(trustMark);
    final TrustMarkIssuer issuer = this.issuer(
        new TrustMarkSubjectProperty(SUBJECT, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60), false));

    Assertions.assertEquals(trustMark.serialize(), issuer.trustMark(new TrustMarkRequest(TYPE, SUBJECT)));
  }

  @Test
  void listingOnlyContainsValidSubjects() throws Exception {
    final Instant now = Instant.now();
    Mockito.when(this.source.getTrustMarkSubjects(Mockito.any(), Mockito.any())).thenReturn(List.of(
        new TrustMarkSubjectProperty("https://valid.example.com", now.minusSeconds(60), now.plusSeconds(60), false),
        new TrustMarkSubjectProperty("https://revoked.example.com", null, null, true),
        new TrustMarkSubjectProperty("https://expired.example.com", null, now.minusSeconds(60), false),
        new TrustMarkSubjectProperty("https://future.example.com", now.plusSeconds(60), null, false)));
    final TrustMarkIssuer issuer = this.issuer(new TrustMarkSubjectProperty(SUBJECT, null, null, false));

    Assertions.assertEquals(List.of("https://valid.example.com"),
        issuer.trustMarkListing(new TrustMarkListingRequest(TYPE, null)));
  }

  @Test
  void listingWithoutValidSubjectsIsEmpty() throws Exception {
    Mockito.when(this.source.getTrustMarkSubjects(Mockito.any(), Mockito.any())).thenReturn(List.of(
        new TrustMarkSubjectProperty("https://revoked.example.com", null, null, true)));
    final TrustMarkIssuer issuer = this.issuer(new TrustMarkSubjectProperty(SUBJECT, null, null, false));

    Assertions.assertEquals(List.of(), issuer.trustMarkListing(new TrustMarkListingRequest(TYPE, null)));
  }

  @Test
  void listingForUnknownTypeIsNotFound() {
    final TrustMarkIssuer issuer = this.issuer(new TrustMarkSubjectProperty(SUBJECT, null, null, false));
    Assertions.assertThrows(NotFoundException.class,
        () -> issuer.trustMarkListing(new TrustMarkListingRequest("http://tm.digg.se/other", null)));
  }

  @Test
  void signerRefusesExpInThePast() throws Exception {
    final TrustMarkSigner realSigner = new TrustMarkSigner(Mockito.mock(SignerFactory.class), Clock.systemUTC());
    final TrustMarkIssuerProperties properties = this.properties(List.of());
    final TrustMarkSubjectProperty expired =
        new TrustMarkSubjectProperty(SUBJECT, null, Instant.now().minusSeconds(60), false);
    Assertions.assertThrows(ServerErrorException.class, () -> realSigner.sign(
        Mockito.mock(EntityRecord.class), properties, properties.trustMarks().getFirst(), expired));
  }

  private TrustMarkIssuer issuer(final TrustMarkSubjectProperty subject) {
    return new TrustMarkIssuer(this.properties(List.of(subject)), this.signer, this.source, Clock.systemUTC());
  }

  private TrustMarkIssuerProperties properties(final List<TrustMarkSubjectProperty> subjects) {
    final List<TrustMarkProperties> trustMarks = new ArrayList<>();
    trustMarks.add(TrustMarkProperties.builder()
        .trustMarkType(TrustMarkType.create(TYPE))
        .trustMarkSubjects(subjects)
        .build());
    return TrustMarkIssuerProperties.builder()
        .trustMarkValidityDuration(Duration.ofMinutes(5))
        .entityIdentifier(new EntityID("https://tmissuer.digg.se"))
        .trustMarks(trustMarks)
        .build();
  }
}
