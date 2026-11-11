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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.UnsupportedParameterException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link DefaultTrustAnchor#subordinateListing(SubordinateListingRequest)}.
 *
 * @author Martin Lindström
 */
class DefaultTrustAnchorListingTest {

  private static final String TA = "https://ta.example.com";
  private static final String IE = "https://ie.example.com";
  private static final String OP = "https://op.example.com";
  private static final String RP = "https://rp.example.com";
  private static final String TYPE = "https://example.com/tm/certified";
  private static final String OTHER_TYPE = "https://example.com/tm/other";

  private CompositeRecordSource source;

  @BeforeEach
  void setUp() {
    this.source = Mockito.mock(CompositeRecordSource.class);
    Mockito.when(this.source.getEntity(Mockito.any())).thenReturn(Optional.empty());
    Mockito.when(this.source.getTrustMarkIssuerProperties()).thenReturn(List.of());
  }

  @Test
  void noFilterListsAllOnce() throws Exception {
    Assertions.assertEquals(List.of(IE, OP, RP), this.list(SubordinateListingRequest.requestAll()));
  }

  @Test
  void entityTypeFilterUsesRecords() throws Exception {
    Assertions.assertEquals(List.of(OP), this.list(new SubordinateListingRequest(
        List.of("openid_provider"), null, null, null)));
    Assertions.assertEquals(List.of(OP, RP), this.list(new SubordinateListingRequest(
        List.of("openid_provider", "openid_relying_party"), null, null, null)));
  }

  @Test
  void intermediateFilterUsesRecords() throws Exception {
    Assertions.assertEquals(List.of(IE), this.list(new SubordinateListingRequest(null, null, null, true)));
    Assertions.assertEquals(List.of(IE, OP, RP), this.list(new SubordinateListingRequest(null, null, null, false)));
  }

  @Test
  void trustMarkFiltersAreUnsupportedWhenNotTrustMarkIssuer() throws Exception {
    final UnsupportedParameterException e = Assertions.assertThrows(UnsupportedParameterException.class,
        () -> this.list(new SubordinateListingRequest(null, null, TYPE, null)));
    Assertions.assertEquals("unsupported_parameter", e.toJSONObject().get("error"));
    Assertions.assertThrows(UnsupportedParameterException.class,
        () -> this.list(new SubordinateListingRequest(null, true, null, null)));
    Assertions.assertEquals(List.of(IE, OP, RP), this.list(new SubordinateListingRequest(null, false, null, null)));
  }

  @Test
  void trustMarkTypeFilterUsesTrustMarkSubjects() throws Exception {
    this.trustMarkIssuer();
    Assertions.assertEquals(List.of(OP), this.list(new SubordinateListingRequest(null, null, TYPE, null)));
  }

  @Test
  void trustMarkedFilterUsesAllTypes() throws Exception {
    this.trustMarkIssuer();
    Assertions.assertEquals(List.of(OP, RP), this.list(new SubordinateListingRequest(null, true, null, null)));
  }

  @Test
  void trustMarkFilterIsCombinedWithEntityType() throws Exception {
    this.trustMarkIssuer();
    Assertions.assertEquals(List.of(RP), this.list(new SubordinateListingRequest(
        List.of("openid_relying_party"), true, null, null)));
  }

  @Test
  void unknownTrustMarkTypeIsInvalid() throws Exception {
    this.trustMarkIssuer();
    final InvalidRequestException e = Assertions.assertThrows(InvalidRequestException.class,
        () -> this.list(new SubordinateListingRequest(null, null, "https://example.com/tm/unknown", null)));
    Assertions.assertEquals("trust_mark_type 'https://example.com/tm/unknown' is not issued by this entity",
        e.toJSONObject().get("error_description"));
  }

  /**
   * Makes the Trust Anchor a Trust Mark Issuer. The OP holds {@code TYPE}, the RP holds {@code OTHER_TYPE}, a revoked
   * subject and a subject that is not a subordinate hold {@code TYPE}.
   */
  private void trustMarkIssuer() {
    final TrustMarkIssuerProperties tmi = TrustMarkIssuerProperties.builder()
        .entityIdentifier(new EntityID(TA))
        .trustMarks(List.of(
            TrustMarkProperties.builder().trustMarkType(new TrustMarkType(TYPE)).build(),
            TrustMarkProperties.builder().trustMarkType(new TrustMarkType(OTHER_TYPE)).build()))
        .build();
    Mockito.when(this.source.getTrustMarkIssuerProperties()).thenReturn(List.of(tmi));
    Mockito.when(this.source.getTrustMarkSubjects(new EntityID(TA), new TrustMarkType(TYPE))).thenReturn(List.of(
        this.subject(OP, false),
        this.subject(IE, true),
        this.subject("https://not-a-subordinate.example.com", false)));
    Mockito.when(this.source.getTrustMarkSubjects(new EntityID(TA), new TrustMarkType(OTHER_TYPE)))
        .thenReturn(List.of(this.subject(RP, false)));
  }

  private TrustMarkSubjectProperty subject(final String sub, final boolean revoked) {
    return new TrustMarkSubjectProperty(sub, Instant.now().minusSeconds(60), null, revoked);
  }

  private List<String> list(final SubordinateListingRequest request) throws Exception {
    final TrustAnchorProperties properties = new TrustAnchorProperties(new EntityID(TA), null, null);
    properties.setSubordinates(List.of(
        this.subordinate(IE, List.of("federation_entity"), true),
        this.subordinate(OP, List.of("federation_entity", "openid_provider"), null),
        this.subordinate(RP, List.of("openid_relying_party"), null),
        this.subordinate(OP, List.of("federation_entity", "openid_provider"), null)));
    final DefaultTrustAnchor trustAnchor = new DefaultTrustAnchor(this.source, properties,
        Mockito.mock(SubordinateStatementFactory.class));
    return trustAnchor.subordinateListing(request);
  }

  private TrustAnchorProperties.SubordinateListingProperty subordinate(
      final String id, final List<String> entityTypes, final Boolean intermediate) {
    return TrustAnchorProperties.SubordinateListingProperty.builder()
        .entityIdentifier(new EntityID(id))
        .entityTypes(entityTypes)
        .intermediate(intermediate)
        .build();
  }
}
