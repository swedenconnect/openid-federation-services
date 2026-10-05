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
package se.swedenconnect.oidf.common.entity.tree.scraping;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;

import java.util.List;
import java.util.Map;

/**
 * Tests for how {@link ScrapedEntity} fetches trust mark statuses.
 *
 * @author Martin Lindström
 */
class ScrapedEntityTrustMarkStatusTest {

  private static final String SUBJECT = "https://example.com/entity";
  private static final String ISSUER = "https://example.com/tmi";
  private static final String TYPE = "https://example.com/tm/1";
  private static final String STATUS_ENDPOINT = "https://example.com/tmi/custom/status";

  private ECKey key;
  private FederationClient client;
  private SignedJWT trustMark;

  @BeforeEach
  void setUp() throws Exception {
    this.key = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
    this.client = Mockito.mock(FederationClient.class);
  }

  @Test
  void statusEndpointIsReadFromIssuerMetadata() throws Exception {
    this.mockConfigurations(Map.of("federation_trust_mark_status_endpoint", STATUS_ENDPOINT));
    final TrustMarkStatusResponse response = new TrustMarkStatusResponse(null, false);
    Mockito.when(this.client.trustMarkStatus(Mockito.any())).thenReturn(response);

    final ScrapedEntity entity = this.scrape();

    @SuppressWarnings("unchecked")
    final ArgumentCaptor<FederationRequest<FederationTrustMarkStatusRequest>> captor =
        ArgumentCaptor.forClass(FederationRequest.class);
    Mockito.verify(this.client).trustMarkStatus(captor.capture());
    Assertions.assertEquals(STATUS_ENDPOINT,
        captor.getValue().federationEntityMetadata().get("federation_trust_mark_status_endpoint"));
    Assertions.assertSame(response, entity.getTrustMarkStatuses().get(this.trustMark.serialize()));
  }

  @Test
  void issuerWithoutStatusEndpointIsMarked() throws Exception {
    this.mockConfigurations(Map.of("organization_name", "TMI"));

    final ScrapedEntity entity = this.scrape();

    Mockito.verify(this.client, Mockito.never()).trustMarkStatus(Mockito.any());
    Assertions.assertTrue(entity.getTrustMarkStatuses().get(this.trustMark.serialize()).isNoStatusEndpoint());
  }

  @Test
  void unreachableIssuerGivesError() throws Exception {
    final SignedJWT leaf = this.leafConfiguration();
    Mockito.when(this.client.entityConfiguration(Mockito.any())).thenAnswer(invocation -> {
      if (isIssuer(invocation.getArgument(0))) {
        throw new IllegalStateException("connection refused");
      }
      return leaf;
    });

    final ScrapedEntity entity = this.scrape();

    Mockito.verify(this.client, Mockito.never()).trustMarkStatus(Mockito.any());
    final TrustMarkStatusResponse status = entity.getTrustMarkStatuses().get(this.trustMark.serialize());
    Assertions.assertTrue(status.isError());
    Assertions.assertFalse(status.isNoStatusEndpoint());
  }

  private static boolean isIssuer(final FederationRequest<EntityConfigurationRequest> request) {
    return ISSUER.equals(request.parameters().entityID().getValue());
  }

  private ScrapedEntity scrape() {
    final ScrapedEntity entity = ScrapedEntity.builder().entityID(new EntityID(SUBJECT)).build();
    entity.scrape(this.client);
    return entity;
  }

  private void mockConfigurations(final Map<String, Object> issuerMetadata) throws Exception {
    final SignedJWT leaf = this.leafConfiguration();
    final SignedJWT issuer = this.configuration(ISSUER, issuerMetadata, null);
    Mockito.when(this.client.entityConfiguration(Mockito.any())).thenAnswer(invocation -> {
      return isIssuer(invocation.getArgument(0)) ? issuer : leaf;
    });
  }

  private SignedJWT leafConfiguration() throws Exception {
    this.trustMark = this.sign("trust-mark+jwt", new JWTClaimsSet.Builder()
        .issuer(ISSUER)
        .subject(SUBJECT)
        .claim("trust_mark_type", TYPE)
        .build());
    final JSONObject entry = new JSONObject();
    entry.put("trust_mark_type", TYPE);
    entry.put("trust_mark", this.trustMark.serialize());
    return this.configuration(SUBJECT, Map.of("organization_name", "Leaf"), List.of(entry));
  }

  private SignedJWT configuration(final String entityId, final Map<String, Object> federationEntity,
      final List<Object> trustMarks) throws Exception {
    final JSONObject metadata = new JSONObject();
    metadata.put("federation_entity", new JSONObject(federationEntity));
    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
        .issuer(entityId)
        .subject(entityId)
        .claim("jwks", new JSONObject(new JWKSet(this.key.toPublicJWK()).toJSONObject()))
        .claim("metadata", metadata);
    if (trustMarks != null) {
      claims.claim("trust_marks", trustMarks);
    }
    return this.sign("entity-statement+jwt", claims.build());
  }

  private SignedJWT sign(final String typ, final JWTClaimsSet claims) throws Exception {
    final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(new JOSEObjectType(typ))
        .keyID(this.key.getKeyID())
        .build(), claims);
    jwt.sign(new ECDSASigner(this.key));
    return jwt;
  }
}
