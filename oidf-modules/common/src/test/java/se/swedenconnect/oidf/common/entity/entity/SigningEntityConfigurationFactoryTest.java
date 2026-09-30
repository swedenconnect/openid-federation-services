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
package se.swedenconnect.oidf.common.entity.entity;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;
import java.util.Map;

class SigningEntityConfigurationFactoryTest {

  private static final EntityID ENTITY_ID = new EntityID("https://example.com/entity");

  private JWKSet jwks;

  private SigningEntityConfigurationFactory factory;

  @BeforeEach
  void setUp() throws Exception {
    this.jwks = new JWKSet(new ECKeyGenerator(Curve.P_256).keyID("test-key").generate());
    this.factory = new SigningEntityConfigurationFactory(Mockito.mock(FederationClient.class), List.of());
  }

  @Test
  void omitsAuthorityHintsWhenNotConfigured() throws Exception {
    final JWTClaimsSet claims = this.create(this.recordBuilder().build());

    Assertions.assertFalse(claims.getClaims().containsKey("authority_hints"));
  }

  @Test
  void omitsAuthorityHintsWhenEmpty() throws Exception {
    final JWTClaimsSet claims = this.create(this.recordBuilder().authorityHints(List.of()).build());

    Assertions.assertFalse(claims.getClaims().containsKey("authority_hints"));
  }

  @Test
  void includesAuthorityHintsWhenNotEmpty() throws Exception {
    final List<String> hints = List.of("https://example.com/ta");
    final JWTClaimsSet claims = this.create(this.recordBuilder().authorityHints(hints).build());

    Assertions.assertEquals(hints, claims.getStringListClaim("authority_hints"));
  }

  @Test
  void omitsCritWhenNotConfigured() throws Exception {
    final JWTClaimsSet claims = this.create(this.recordBuilder().build());

    Assertions.assertFalse(claims.getClaims().containsKey("crit"));
  }

  @Test
  void omitsCritWhenEmpty() throws Exception {
    final JWTClaimsSet claims = this.create(this.recordBuilder().crit(List.of()).build());

    Assertions.assertFalse(claims.getClaims().containsKey("crit"));
  }

  @Test
  void includesCritWhenNotEmpty() throws Exception {
    final List<String> crit = List.of("ext_a", "ext_b");
    final JWTClaimsSet claims = this.create(this.recordBuilder().crit(crit).build());

    Assertions.assertEquals(crit, claims.getStringListClaim("crit"));
  }

  @Test
  void signedPayloadNeverContainsEmptyArrays() throws Exception {
    final EntityRecord record = this.recordBuilder()
        .authorityHints(List.of())
        .crit(List.of())
        .build();

    final String payload = this.factory.createEntityConfiguration(record).getPayload().toString();

    Assertions.assertFalse(payload.contains("\"authority_hints\""));
    Assertions.assertFalse(payload.contains("\"crit\""));
  }

  private EntityRecord.EntityRecordBuilder recordBuilder() {
    return EntityRecord.builder()
        .entityIdentifier(ENTITY_ID)
        .metadata(Map.of("federation_entity", Map.of("organization_name", "Test")))
        .jwks(this.jwks);
  }

  private JWTClaimsSet create(final EntityRecord record) throws Exception {
    return this.factory.createEntityConfiguration(record).getJWTClaimsSet();
  }
}
