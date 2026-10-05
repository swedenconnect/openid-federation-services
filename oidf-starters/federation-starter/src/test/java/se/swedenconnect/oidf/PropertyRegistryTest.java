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
package se.swedenconnect.oidf;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;
import java.util.Map;

/**
 * Tests for the {@code ec-location} checks of {@link PropertyRegistry#validate(String, EcLocationValidator)}.
 */
class PropertyRegistryTest {

  private static final EntityID TA = new EntityID("https://example.com/ta");
  private static final EntityID LEAF = new EntityID("https://example.com/leaf");

  @Test
  void subordinateWithHttpEcLocationIsRejected() {
    final PropertyRegistry registry = this.registry("http://hosting.example.com/leaf/ec", null);

    final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> registry.validate("federation.local-registry", new EcLocationValidator(false)));
    Assertions.assertTrue(e.getMessage().contains(LEAF.getValue()), e.getMessage());
  }

  @Test
  void subordinateWithHttpEcLocationIsAcceptedWhenAllowed() {
    final PropertyRegistry registry = this.registry("http://hosting.example.com/leaf/ec", null);

    Assertions.assertDoesNotThrow(() -> registry.validate("federation.local-registry", new EcLocationValidator(true)));
  }

  @Test
  void entityWithBase64DataUrlIsRejected() {
    final PropertyRegistry registry = this.registry(null, "data:application/entity-statement+jwt;base64,e30");

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> registry.validate("federation.local-registry", new EcLocationValidator(false)));
  }

  @Test
  void relativeEcLocationIsAccepted() {
    final PropertyRegistry registry = this.registry("/hosted/ec", "/hosted/ec");

    Assertions.assertDoesNotThrow(() -> registry.validate("federation.local-registry", new EcLocationValidator(false)));
  }

  private PropertyRegistry registry(final String subordinateEcLocation, final String entityEcLocation) {
    final TrustAnchorProperties trustAnchor = new TrustAnchorProperties(TA, Map.of(), List.of());
    trustAnchor.setSubordinates(List.of(TrustAnchorProperties.SubordinateListingProperty.builder()
        .entityIdentifier(LEAF)
        .ecLocation(subordinateEcLocation)
        .build()));
    return new PropertyRegistry(List.of(), List.of(trustAnchor), List.of(), List.of(
        EntityRecord.builder().entityIdentifier(TA).build(),
        EntityRecord.builder().entityIdentifier(LEAF).ecLocation(entityEcLocation).build()));
  }
}
