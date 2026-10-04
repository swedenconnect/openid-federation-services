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
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Tests for {@link TrustMarkIssuer#trustMarkStatus(TrustMarkStatusRequest)}.
 *
 * @author Martin Lindström
 */
class TrustMarkIssuerStatusTest {

  private static final String ISSUER = "https://tmissuer.digg.se";
  private static final String TYPE = "http://tm.digg.se/default";
  private static final String SUBJECT = "http://tm1.digg.se/sub1";

  private RSAKey key;
  private TrustMarkSigner signer;
  private CompositeRecordSource source;
  private TrustMarkIssuer issuer;

  @BeforeEach
  void setUp() throws Exception {
    this.key = new RSAKeyGenerator(2048).keyID("key").generate();
    final TrustMarkIssuerProperties properties = TestDataSetup.trustMarkProperties();
    this.signer = Mockito.mock(TrustMarkSigner.class);
    this.source = Mockito.mock(CompositeRecordSource.class);
    final EntityRecord entity = Mockito.mock(EntityRecord.class);
    Mockito.when(this.source.getEntity(Mockito.any())).thenReturn(Optional.of(entity));
    Mockito.when(this.signer.verify(Mockito.any(), Mockito.any())).thenReturn(true);
    Mockito.when(this.signer.signStatus(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(invocation -> this.sign(new JWTClaimsSet.Builder()
            .claim("status", invocation.getArgument(2, String.class))
            .build()));
    this.issuer = new TrustMarkIssuer(properties, this.signer, this.source, Clock.systemUTC());
  }

  @Test
  void activeTrustMark() throws Exception {
    this.registerSubject(false);
    Assertions.assertEquals("active", this.status(this.trustMark(ISSUER, TYPE, Instant.now().plusSeconds(60))));
  }

  @Test
  void revokedTrustMark() throws Exception {
    this.registerSubject(true);
    Assertions.assertEquals("revoked", this.status(this.trustMark(ISSUER, TYPE, Instant.now().plusSeconds(60))));
  }

  @Test
  void expiredTrustMark() throws Exception {
    this.registerSubject(false);
    Assertions.assertEquals("expired", this.status(this.trustMark(ISSUER, TYPE, Instant.now().minusSeconds(60))));
  }

  @Test
  void invalidSignature() throws Exception {
    this.registerSubject(false);
    Mockito.when(this.signer.verify(Mockito.any(), Mockito.any())).thenReturn(false);
    Assertions.assertEquals("invalid", this.status(this.trustMark(ISSUER, TYPE, Instant.now().plusSeconds(60))));
  }

  @Test
  void unknownSubjectIsNotFound() throws Exception {
    Mockito.when(this.source.getTrustMarkSubject(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.empty());
    final String trustMark = this.trustMark(ISSUER, TYPE, Instant.now().minusSeconds(60));
    Assertions.assertThrows(NotFoundException.class, () -> this.status(trustMark));
  }

  @Test
  void otherIssuerIsNotFound() throws Exception {
    this.registerSubject(false);
    final String trustMark = this.trustMark("https://other.example.com", TYPE, Instant.now().plusSeconds(60));
    Assertions.assertThrows(NotFoundException.class, () -> this.status(trustMark));
  }

  @Test
  void unknownTypeIsNotFound() throws Exception {
    this.registerSubject(false);
    final String trustMark = this.trustMark(ISSUER, "http://tm.digg.se/other", Instant.now().plusSeconds(60));
    Assertions.assertThrows(NotFoundException.class, () -> this.status(trustMark));
  }

  @Test
  void malformedTrustMarkIsInvalidRequest() {
    Assertions.assertThrows(InvalidRequestException.class,
        () -> this.issuer.trustMarkStatus(new TrustMarkStatusRequest("not-a-jwt")));
  }

  private void registerSubject(final boolean revoked) {
    Mockito.when(this.source.getTrustMarkSubject(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.of(new TrustMarkSubjectProperty(SUBJECT, Instant.now(), null, revoked)));
  }

  private String status(final String trustMark) throws Exception {
    return SignedJWT.parse(this.issuer.trustMarkStatus(new TrustMarkStatusRequest(trustMark)))
        .getJWTClaimsSet().getStringClaim("status");
  }

  private String trustMark(final String iss, final String type, final Instant expiration) throws Exception {
    return this.sign(new JWTClaimsSet.Builder()
        .issuer(iss)
        .subject(SUBJECT)
        .claim("trust_mark_type", type)
        .issueTime(new Date())
        .expirationTime(Date.from(expiration))
        .build()).serialize();
  }

  private SignedJWT sign(final JWTClaimsSet claims) throws Exception {
    final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    jwt.sign(new RSASSASigner(this.key));
    return jwt;
  }
}
