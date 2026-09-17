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
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.jwt.FederationSigner;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubordinateStatementFactoryTest {

  private static final EntityID ISSUER_ID = new EntityID("https://example.com/ta");
  private static final EntityID SUBORDINATE_ID = new EntityID("https://example.com/im");

  @Mock
  private SignerFactory signerFactory;

  @Mock
  private FederationSigner signer;

  private JWKSet subordinateJwks;

  @BeforeEach
  void setUp() throws Exception {
    final JWK key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    this.subordinateJwks = new JWKSet(key.toPublicJWK());
    when(this.signerFactory.createSigner(any())).thenReturn(this.signer);
  }

  @Test
  void includesMetadataClaimWhenConfigured() throws Exception {
    final Map<String, Object> metadata = Map.of(
        "openid_relying_party", Map.of("organization_name", "Test RP")
    );

    final TrustAnchorProperties.SubordinateListingProperty subordinate =
        TrustAnchorProperties.SubordinateListingProperty.builder()
            .entityIdentifier(SUBORDINATE_ID)
            .jwks(this.subordinateJwks)
            .metadata(metadata)
            .build();

    final JWTClaimsSet capturedClaims = this.sign(subordinate);

    Assertions.assertEquals(metadata, capturedClaims.getClaim("metadata"));
  }

  @Test
  void omitsMetadataClaimWhenNotConfigured() throws Exception {
    final TrustAnchorProperties.SubordinateListingProperty subordinate =
        TrustAnchorProperties.SubordinateListingProperty.builder()
            .entityIdentifier(SUBORDINATE_ID)
            .jwks(this.subordinateJwks)
            .build();

    final JWTClaimsSet capturedClaims = this.sign(subordinate);

    Assertions.assertFalse(capturedClaims.getClaims().containsKey("metadata"));
  }

  private JWTClaimsSet sign(final TrustAnchorProperties.SubordinateListingProperty subordinate) throws Exception {
    final EntityRecord issuer = EntityRecord.builder()
        .entityIdentifier(ISSUER_ID)
        .build();

    final ArgumentCaptor<JWTClaimsSet> claimsCaptor = ArgumentCaptor.forClass(JWTClaimsSet.class);
    when(this.signer.sign(any(JOSEObjectType.class), claimsCaptor.capture()))
        .thenReturn(org.mockito.Mockito.mock(SignedJWT.class));

    new SubordinateStatementFactory(this.signerFactory).createEntityStatement(issuer, subordinate);

    return claimsCaptor.getValue();
  }
}
