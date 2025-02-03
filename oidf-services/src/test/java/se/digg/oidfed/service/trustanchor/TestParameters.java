/*
 * Copyright 2024-2025 Sweden Connect
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
package se.digg.oidfed.service.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.web.client.HttpClientErrorException;
import se.digg.oidfed.service.entity.TestFederationEntities;
import se.digg.oidfed.trustanchor.SubordinateListingRequest;

import java.util.List;
import java.util.stream.Stream;

import static se.digg.oidfed.service.trustanchor.TrustAnchorIT.TRUST_MARK_ID;

public class TestParameters {
  static Stream<Arguments> okSubordinateListingParameters() {
    return Stream.of(
        // Request - expected hits
        Arguments.of(
            requestOf(null, null, null, null), List.of(
                TestFederationEntities.PrivateSector.TRUST_ANCHOR,
                TestFederationEntities.Authorization.OP_1,
                TestFederationEntities.Authorization.OP_2
            )
        ),
        Arguments.of(
            requestOf(EntityType.OPENID_PROVIDER.getValue(), null, null, null), List.of(
                TestFederationEntities.Authorization.OP_1,
                TestFederationEntities.Authorization.OP_2
            )
        ),
        Arguments.of(
            requestOf(null, true, null, null), List.of(
                TestFederationEntities.Authorization.OP_1
            )
        ),
        Arguments.of(
            requestOf(null, null, TRUST_MARK_ID, null), List.of(
                TestFederationEntities.Authorization.OP_1
            )
        ),
        Arguments.of(
            requestOf(null, null, null, true), List.of(
                TestFederationEntities.PrivateSector.TRUST_ANCHOR
            )
        ),
        Arguments.of(
            requestOf(EntityType.OPENID_PROVIDER.getValue(), true, TRUST_MARK_ID, null), List.of(
                TestFederationEntities.Authorization.OP_1
            )
        ),
        Arguments.of(
            requestOf(null, false, null, null), List.of(
                TestFederationEntities.PrivateSector.TRUST_ANCHOR,
                TestFederationEntities.Authorization.OP_2
            )
        ),
        Arguments.of(
            requestOf(null, null, null, false), List.of(
                TestFederationEntities.Authorization.OP_1,
                TestFederationEntities.Authorization.OP_2
            )
        )
    );
  }

  static Stream<Arguments> nokSubordinateListingParameters() {
    return Stream.of(
        // Request - expected hits
        Arguments.of(
            requestOf(null, null, "https://non-existant-trust-mark", null), HttpClientErrorException.NotFound.class
        )
    );
  }

  private static SubordinateListingRequest requestOf(final String entityType, final Boolean trustMarked,
                                                     final String trustMarkId, final Boolean intermediate) {
    return new SubordinateListingRequest(entityType, trustMarked, trustMarkId, intermediate);
  }
}
