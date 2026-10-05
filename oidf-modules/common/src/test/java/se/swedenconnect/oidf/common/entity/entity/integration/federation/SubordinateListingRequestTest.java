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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

/**
 * Tests for {@link SubordinateListingRequest}.
 *
 * @author Martin Lindström
 */
class SubordinateListingRequestTest {

  private static final SignedJWT LEAF = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),
      new JWTClaimsSet.Builder().issuer("https://leaf.example.com").subject("https://leaf.example.com").build());

  @Test
  void falseValuesDoNotFilter() {
    final SubordinateListingRequest request = new SubordinateListingRequest(null, false, null, false);
    Assertions.assertFalse(request.requiresFiltering());
    Assertions.assertTrue(request.toPredicate(es -> List.of()).test(LEAF));
  }

  @Test
  void trueValuesFilter() {
    final Predicate<SignedJWT> trustMarked =
        new SubordinateListingRequest(null, true, null, null).toPredicate(es -> List.of());
    final Predicate<SignedJWT> intermediate =
        new SubordinateListingRequest(null, null, null, true).toPredicate(es -> List.of());
    Assertions.assertFalse(trustMarked.test(LEAF));
    Assertions.assertFalse(intermediate.test(LEAF));
  }

  @Test
  void entityWithOnlyFetchEndpointIsIntermediate() {
    final SignedJWT intermediate = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
        .issuer("https://im.example.com")
        .subject("https://im.example.com")
        .claim("metadata", java.util.Map.of("federation_entity",
            java.util.Map.of("federation_fetch_endpoint", "https://im.example.com/fetch")))
        .build());
    Assertions.assertTrue(new SubordinateListingRequest(null, null, null, true)
        .toPredicate(es -> List.of()).test(intermediate));
  }
}
