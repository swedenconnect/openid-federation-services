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
package se.swedenconnect.oidf.common.entity.entity.integration.properties;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of the validation of the trust mark validity duration, which is optional and may be configured per trust
 * mark as well as on the trust mark issuer.
 */
class TrustMarkValidityDurationTest {

  private static final String TYPE = "https://id.example.com/contract/sc/prepaid-auth-2021";
  private static final EntityID ISSUER = new EntityID("https://fed.example.com/tmi-contracts");

  @Test
  void durationIsOptional() {
    assertDoesNotThrow(() -> issuer(null, null).validate());
  }

  @Test
  void durationOfTrustMarkIsAccepted() {
    assertDoesNotThrow(() -> issuer(null, Duration.ofDays(2)).validate());
  }

  @Test
  void shortDurationOfTrustMarkIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> issuer(null, Duration.ofMinutes(1)).validate());
  }

  @Test
  void durationOfExactlyTheMinimumIsAccepted() {
    assertDoesNotThrow(() -> issuer(Duration.ofMinutes(5), Duration.ofMinutes(5)).validate());
  }

  @Test
  void durationJustUnderTheMinimumIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> issuer(null, Duration.ofMinutes(4).plusSeconds(30)).validate());
  }

  @Test
  void shortDurationsAreOnlyWarnedAboutWhenRecordsAreLoaded() {
    assertDoesNotThrow(() -> issuer(Duration.ofMinutes(1), Duration.ofMinutes(1)).warnForShortValidityDurations());
  }

  @Test
  void warningForShortDurationsHandlesIncompleteConfiguration() {
    assertDoesNotThrow(() -> TrustMarkIssuerProperties.builder()
        .entityIdentifier(ISSUER)
        .build()
        .warnForShortValidityDurations());
    assertDoesNotThrow(() -> TrustMarkIssuerProperties.builder()
        .entityIdentifier(ISSUER)
        .trustMarks(List.of(TrustMarkProperties.builder()
            .trustMarkValidityDuration(Duration.ofMinutes(1))
            .build()))
        .build()
        .warnForShortValidityDurations());
  }

  @Test
  void shortDurationOfIssuerIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> issuer(Duration.ofMinutes(1), null).validate());
  }

  @Test
  void issuerWithoutTrustMarksIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> TrustMarkIssuerProperties.builder()
        .entityIdentifier(ISSUER)
        .trustMarks(List.of())
        .build()
        .validate());
  }

  private static TrustMarkIssuerProperties issuer(final Duration issuerDuration, final Duration trustMarkDuration) {
    return TrustMarkIssuerProperties.builder()
        .entityIdentifier(ISSUER)
        .trustMarkValidityDuration(issuerDuration)
        .trustMarks(List.of(TrustMarkProperties.builder()
            .trustMarkType(TrustMarkType.create(TYPE))
            .trustMarkValidityDuration(trustMarkDuration)
            .build()))
        .build();
  }
}
