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

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for the trust mark filters of {@link DefaultTrustAnchor#subordinateListing(SubordinateListingRequest)}.
 *
 * @author Martin Lindström
 */
class DefaultTrustAnchorListingTest {

  private static final String TA = "https://ta.example.com";
  private static final String ISSUER = "https://tmi.example.com";
  private static final String HOLDER = "https://holder.example.com";
  private static final String TYPE = "https://example.com/tm/certified";

  private final Map<String, ECKey> keys = new HashMap<>();
  private final Map<String, SignedJWT> configurations = new HashMap<>();
  private CompositeRecordSource source;
  private FederationClient client;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    for (final String id : List.of(ISSUER, HOLDER, "https://other.example.com")) {
      this.keys.put(id, new ECKeyGenerator(Curve.P_256).keyID(id + "#key").generate());
    }
    this.source = Mockito.mock(CompositeRecordSource.class);
    Mockito.when(this.source.getEntity(Mockito.any())).thenReturn(Optional.empty());
    this.client = Mockito.mock(FederationClient.class);
    Mockito.when(this.client.entityConfiguration(Mockito.any())).thenAnswer(invocation -> {
      final FederationRequest<EntityConfigurationRequest> request = invocation.getArgument(0);
      final SignedJWT configuration = this.configurations.get(request.parameters().entityID().getValue());
      if (configuration == null) {
        throw new IllegalStateException("connection refused");
      }
      return configuration;
    });
    this.configurations.put(ISSUER, this.configuration(ISSUER, ISSUER, List.of()));
  }

  @Test
  void validTrustMarkFromDirectSubordinateCounts() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark(ISSUER, HOLDER, Instant.now().plusSeconds(600)))));
    Assertions.assertEquals(List.of(HOLDER), this.listByType(null));
  }

  @Test
  void trustMarkFromIssuerOutsideTheTrustAnchorDoesNotCount() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark("https://other.example.com", HOLDER, Instant.now().plusSeconds(600)))));
    Assertions.assertEquals(List.of(), this.listByType(null));
  }

  @Test
  void copiedTrustMarkDoesNotCount() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark(ISSUER, "https://someone-else.example.com", Instant.now().plusSeconds(600)))));
    Assertions.assertEquals(List.of(), this.listByType(null));
  }

  @Test
  void expiredTrustMarkDoesNotCount() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark(ISSUER, HOLDER, Instant.now().minusSeconds(60)))));
    Assertions.assertEquals(List.of(), this.listByType(null));
  }

  @Test
  void issuerNotAllowedByTrustMarkIssuersDoesNotCount() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark(ISSUER, HOLDER, Instant.now().plusSeconds(600)))));
    Assertions.assertEquals(List.of(),
        this.listByType(Map.of(new EntityID(TYPE), List.of(new EntityID("https://other.example.com")))));
  }

  @Test
  void entityConfigurationSignedWithOtherKeyIsSkipped() throws Exception {
    this.configurations.put(HOLDER, this.configuration(HOLDER, "https://other.example.com",
        List.of(this.trustMark(ISSUER, HOLDER, Instant.now().plusSeconds(600)))));
    Assertions.assertEquals(List.of(), this.listByType(null));
  }

  @Test
  void unavailableSubordinateIsKeptForEntityTypeFilter() throws Exception {
    // No Entity Configuration is registered for the holder, so fetching it fails
    Assertions.assertEquals(List.of(HOLDER),
        this.list(new SubordinateListingRequest(List.of("openid_provider"), null, null, null), null));
  }

  @Test
  void duplicateSubordinatesAreListedOnce() throws Exception {
    Assertions.assertEquals(List.of(ISSUER, HOLDER), this.list(SubordinateListingRequest.requestAll(), null));
  }

  @Test
  void unavailableSubordinateIsLeftOutForTrustMarkFilter() throws Exception {
    Assertions.assertEquals(List.of(), this.listByType(null));
  }

  @Test
  void relativeEcLocationIsResolvedForFilteredListing() throws Exception {
    final SignedJWT holderConfiguration = this.configuration(HOLDER, HOLDER,
        List.of(this.trustMark(ISSUER, HOLDER, Instant.now().plusSeconds(600))));
    Mockito.doReturn(holderConfiguration).when(this.client).entityConfiguration(Mockito.argThat(request ->
        request != null && (HOLDER + "/hosted/ec").equals(request.parameters().ecLocation())));
    final TrustAnchorProperties.SubordinateListingProperty holder = this.subordinate(HOLDER);
    holder.setEcLocation("/hosted/ec");

    Assertions.assertEquals(List.of(HOLDER), this.list(new SubordinateListingRequest(null, null, TYPE, null), null,
        List.of(this.subordinate(ISSUER), holder)));
  }

  private List<String> listByType(final Map<EntityID, List<EntityID>> trustMarkIssuers) throws Exception {
    return this.list(new SubordinateListingRequest(null, null, TYPE, null), trustMarkIssuers);
  }

  private List<String> list(final SubordinateListingRequest request,
      final Map<EntityID, List<EntityID>> trustMarkIssuers) throws Exception {
    return this.list(request, trustMarkIssuers,
        List.of(this.subordinate(ISSUER), this.subordinate(HOLDER), this.subordinate(HOLDER)));
  }

  private List<String> list(final SubordinateListingRequest request,
      final Map<EntityID, List<EntityID>> trustMarkIssuers,
      final List<TrustAnchorProperties.SubordinateListingProperty> subordinates) throws Exception {
    final TrustAnchorProperties properties = new TrustAnchorProperties(new EntityID(TA), trustMarkIssuers, null);
    properties.setSubordinates(subordinates);
    final DefaultTrustAnchor trustAnchor = new DefaultTrustAnchor(this.source, properties,
        Mockito.mock(SubordinateStatementFactory.class), this.client);
    return trustAnchor.subordinateListing(request);
  }

  private TrustAnchorProperties.SubordinateListingProperty subordinate(final String id) {
    return TrustAnchorProperties.SubordinateListingProperty.builder()
        .entityIdentifier(new EntityID(id))
        .jwks(new JWKSet(this.keys.get(id).toPublicJWK()))
        .build();
  }

  private SignedJWT configuration(final String id, final String signer, final List<SignedJWT> trustMarks)
      throws Exception {
    final List<Object> entries = trustMarks.stream().map(tm -> (Object) new JSONObject(
        Map.of("trust_mark_type", TYPE, "trust_mark", tm.serialize()))).toList();
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(id)
        .subject(id)
        .issueTime(new Date())
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("jwks", new JSONObject(new JWKSet(this.keys.get(signer).toPublicJWK()).toJSONObject()))
        .claim("trust_marks", entries)
        .build();
    return this.sign(signer, "entity-statement+jwt", claims);
  }

  private SignedJWT trustMark(final String issuer, final String subject, final Instant expiration) throws Exception {
    final JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .claim("trust_mark_type", TYPE)
        .issueTime(new Date())
        .expirationTime(Date.from(expiration))
        .build();
    return this.sign(issuer, "trust-mark+jwt", claims);
  }

  private SignedJWT sign(final String signer, final String typ, final JWTClaimsSet claims) throws Exception {
    final ECKey key = this.keys.get(signer);
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(new JOSEObjectType(typ))
        .keyID(key.getKeyID())
        .build(), claims);
    jwt.sign(new ECDSASigner(key));
    return jwt;
  }
}
