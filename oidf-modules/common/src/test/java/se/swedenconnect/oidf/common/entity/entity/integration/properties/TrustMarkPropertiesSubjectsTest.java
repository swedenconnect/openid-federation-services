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
import se.swedenconnect.oidf.common.entity.entity.integration.LocalRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.LocalRegistryProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for a trust mark that is configured without subjects.
 */
class TrustMarkPropertiesSubjectsTest {

  private static final String TYPE = "https://id.example.com/contract/sc/prepaid-auth-2021";
  private static final EntityID ISSUER = new EntityID("https://fed.example.com/tmi-contracts");

  @Test
  void subjectsAreEmptyWhenNotConfigured() {
    final TrustMarkProperties trustMark = TrustMarkProperties.builder()
        .trustMarkType(TrustMarkType.create(TYPE))
        .build();

    assertTrue(trustMark.getTrustMarkSubjects().isEmpty());
  }

  @Test
  void listingATrustMarkWithoutSubjectsIsEmpty() {
    final TrustMarkIssuerProperties issuer = TrustMarkIssuerProperties.builder()
        .trustMarkValidityDuration(Duration.ofMinutes(5))
        .entityIdentifier(ISSUER)
        .trustMarks(List.of(TrustMarkProperties.builder()
            .trustMarkType(TrustMarkType.create(TYPE))
            .build()))
        .build();
    final LocalRecordSource source = new LocalRecordSource(
        new LocalRegistryProperties(List.of(issuer), List.of(), List.of(), List.of()));

    assertEquals(List.of(), source.getTrustMarkSubjects(ISSUER, TrustMarkType.create(TYPE)));
  }
}
