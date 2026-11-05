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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.ServerErrorException;
import se.swedenconnect.oidf.common.entity.jwt.FederationSigner;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Tests of the exp of a signed trust mark: the validity duration of the trust mark, the duration of the trust mark
 * issuer as a fallback, and no exp at all when neither is configured.
 *
 * @author Per Fredrik Plars
 */
class TrustMarkSignerExpirationTest {

  private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String ISSUER = "https://tmissuer.digg.se";
  private static final String TYPE = "http://tm.digg.se/default";
  private static final String SUBJECT = "http://tm1.digg.se/sub1";

  @Test
  void durationOfTrustMarkIsUsed() throws Exception {
    final JWTClaimsSet claims = this.sign(Duration.ofMinutes(30), Duration.ofDays(2), null);

    Assertions.assertEquals(Date.from(NOW.plus(Duration.ofDays(2))), claims.getExpirationTime());
  }

  @Test
  void durationOfIssuerIsUsedAsFallback() throws Exception {
    final JWTClaimsSet claims = this.sign(Duration.ofMinutes(30), null, null);

    Assertions.assertEquals(Date.from(NOW.plus(Duration.ofMinutes(30))), claims.getExpirationTime());
  }

  @Test
  void trustMarkWithoutDurationNeverExpires() throws Exception {
    final JWTClaimsSet claims = this.sign(null, null, null);

    Assertions.assertNull(claims.getExpirationTime());
  }

  @Test
  void expiryOfSubjectIsUsedWhenItIsEarlier() throws Exception {
    final JWTClaimsSet claims = this.sign(null, Duration.ofDays(2), NOW.plus(Duration.ofHours(1)));

    Assertions.assertEquals(Date.from(NOW.plus(Duration.ofHours(1))), claims.getExpirationTime());
  }

  @Test
  void durationIsUsedWhenItIsEarlierThanTheExpiryOfTheSubject() throws Exception {
    final JWTClaimsSet claims = this.sign(null, Duration.ofMinutes(30), NOW.plus(Duration.ofDays(2)));

    Assertions.assertEquals(Date.from(NOW.plus(Duration.ofMinutes(30))), claims.getExpirationTime());
  }

  @Test
  void expiryOfSubjectIsUsedWithoutDuration() throws Exception {
    final JWTClaimsSet claims = this.sign(null, null, NOW.plus(Duration.ofHours(1)));

    Assertions.assertEquals(Date.from(NOW.plus(Duration.ofHours(1))), claims.getExpirationTime());
  }

  @Test
  void expiryOfSubjectInThePastIsRefusedWithoutDuration() {
    Assertions.assertThrows(ServerErrorException.class,
        () -> this.sign(null, null, NOW.minus(Duration.ofHours(1))));
  }

  private JWTClaimsSet sign(final Duration issuerDuration, final Duration trustMarkDuration,
      final Instant subjectExpires) throws Exception {
    final FederationSigner federationSigner = Mockito.mock(FederationSigner.class);
    final SignerFactory signerFactory = Mockito.mock(SignerFactory.class);
    Mockito.when(signerFactory.createSigner(Mockito.any())).thenReturn(federationSigner);
    final TrustMarkProperties trustMark = TrustMarkProperties.builder()
        .trustMarkType(TrustMarkType.create(TYPE))
        .trustMarkValidityDuration(trustMarkDuration)
        .build();
    final TrustMarkIssuerProperties issuer = TrustMarkIssuerProperties.builder()
        .entityIdentifier(new EntityID(ISSUER))
        .trustMarkValidityDuration(issuerDuration)
        .trustMarks(List.of(trustMark))
        .build();

    new TrustMarkSigner(signerFactory, CLOCK).sign(Mockito.mock(EntityRecord.class), issuer, trustMark,
        new TrustMarkSubjectProperty(SUBJECT, null, subjectExpires, false));

    final ArgumentCaptor<JWTClaimsSet> claims = ArgumentCaptor.forClass(JWTClaimsSet.class);
    Mockito.verify(federationSigner).sign(Mockito.eq(TrustMarkSigner.TRUST_MARK_JWT_TYPE), claims.capture());
    return claims.getValue();
  }
}
