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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;

/**
 * Tests for {@link EcLocationValidator}.
 *
 * @author Martin Lindström
 */
class EcLocationValidatorTest {

  private static String jwt;

  private final EcLocationValidator strict = new EcLocationValidator(false);

  private final EcLocationValidator allowHttp = new EcLocationValidator(true);

  @BeforeAll
  static void createJwt() throws Exception {
    final SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
        new JWTClaimsSet.Builder().issuer("https://leaf.example.com").subject("https://leaf.example.com").build());
    signed.sign(new ECDSASigner(new ECKeyGenerator(Curve.P_256).generate()));
    jwt = signed.serialize();
  }

  @Test
  void httpsUrlIsAccepted() {
    Assertions.assertDoesNotThrow(() -> this.strict.validate("https://hosting.example.com/leaf/ec"));
  }

  @Test
  void dataUrlIsAccepted() {
    Assertions.assertDoesNotThrow(() -> this.strict.validate(EcLocationValidator.DATA_URL_PREFIX + jwt));
  }

  @Test
  void httpUrlIsOnlyAcceptedWhenAllowed() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.strict.validate("http://hosting.example.com/leaf/ec"));
    Assertions.assertDoesNotThrow(() -> this.allowHttp.validate("http://hosting.example.com/leaf/ec"));
  }

  @Test
  void invalidValuesAreRejected() {
    for (final String value : List.of(
        "",
        "ftp://hosting.example.com/leaf/ec",
        "https:///leaf/ec",
        "/leaf/ec",
        "hosting.example.com/leaf/ec",
        "data:application/entity-statement+jwt,not-a-jwt",
        "data:application/jwt,eyJhbGciOiJFUzI1NiJ9.e30.c2ln")) {
      Assertions.assertThrows(IllegalArgumentException.class, () -> this.allowHttp.validate(value), value);
    }
  }

  @Test
  void base64DataUrlIsRejected() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.strict.validate("data:application/entity-statement+jwt;base64," + jwt));
  }

  @Test
  void relativeValueIsResolvedAgainstEntityIdentifier() {
    Assertions.assertEquals("https://example.com/leaf/hosted/ec",
        EcLocationValidator.resolve("/hosted/ec", "https://example.com/leaf/"));
    Assertions.assertEquals("https://other.example.com/ec",
        EcLocationValidator.resolve("https://other.example.com/ec", "https://example.com/leaf"));
  }

  @Test
  void subordinateEcLocationIsResolvedAgainstVirtualEntityId() {
    final TrustAnchorProperties.SubordinateListingProperty subordinate =
        TrustAnchorProperties.SubordinateListingProperty.builder()
            .entityIdentifier(new EntityID("https://example.com/leaf"))
            .virtualEntityId(new EntityID("https://hosting.example.com/leaf"))
            .ecLocation("/ec")
            .build();
    Assertions.assertEquals("https://hosting.example.com/leaf/ec", subordinate.resolveEcLocation());
  }

  @Test
  void subordinateWithOtherVirtualEntityIdGetsWellKnownLocation() {
    final TrustAnchorProperties.SubordinateListingProperty subordinate =
        TrustAnchorProperties.SubordinateListingProperty.builder()
            .entityIdentifier(new EntityID("https://example.com/leaf"))
            .virtualEntityId(new EntityID("https://hosting.example.com/leaf"))
            .build();
    Assertions.assertEquals("https://hosting.example.com/leaf/.well-known/openid-federation",
        subordinate.resolveEcLocation());
  }

  @Test
  void subordinateWithoutHostingHasNoEcLocation() {
    final TrustAnchorProperties.SubordinateListingProperty subordinate =
        TrustAnchorProperties.SubordinateListingProperty.builder()
            .entityIdentifier(new EntityID("https://example.com/leaf"))
            .virtualEntityId(new EntityID("https://example.com/leaf"))
            .build();
    Assertions.assertNull(subordinate.resolveEcLocation());
    Assertions.assertDoesNotThrow(() -> this.strict.validate(subordinate));
  }

  @Test
  void relativeValueOnHttpEntityIsRejected() {
    final EntityRecord entity = EntityRecord.builder()
        .entityIdentifier(new EntityID("http://localhost:8080/leaf"))
        .ecLocation("/ec")
        .build();
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.strict.validate(entity));
    Assertions.assertDoesNotThrow(() -> this.allowHttp.validate(entity));
  }
}
