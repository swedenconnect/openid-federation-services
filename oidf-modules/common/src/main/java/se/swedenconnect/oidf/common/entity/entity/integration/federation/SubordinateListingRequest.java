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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @param entityType
 * @param trustMarked
 * @param trustMarkType
 * @param intermediate
 * @author Felix Hellman
 */
public record SubordinateListingRequest(String entityType, Boolean trustMarked, String trustMarkType,
    Boolean intermediate) implements Serializable {

  /**
   * @return request without any filters set
   */
  public static SubordinateListingRequest requestAll() {
    return new SubordinateListingRequest(null, null, null, null);
  }

  /**
   * @return true if any parameter is set
   */
  public boolean requiresFiltering() {
    return Stream.of(this.entityType, this.trustMarkType, this.trustMarked, this.intermediate)
        .anyMatch(Objects::nonNull);
  }

  /**
   * Converts the request into a {@link SignedJWT} predicate
   * @return request as predicate
   */
  public Predicate<SignedJWT> toPredicate() {
    final List<Predicate<SignedJWT>> predicates = new ArrayList<>();

    Optional.ofNullable(this.entityType).ifPresent(type -> {
      predicates.add(es -> Objects.nonNull(EntityStatementClaims.getMetadata(es, new EntityType(type))));
    });

    Optional.ofNullable(this.trustMarkType).ifPresent(tmid -> {
      final Predicate<SignedJWT> predicate =
          es -> trustMarksOf(es).stream()
              .anyMatch(tme -> tme.getID().getValue().equals(tmid));
      predicates.add(predicate);
    });

    Optional.ofNullable(this.trustMarked).ifPresent(marked -> {
      if (marked) {
        predicates.add(es -> !trustMarksOf(es).isEmpty());
      } else {
        predicates.add(es -> trustMarksOf(es).isEmpty());
      }
    });

    Optional.ofNullable(this.intermediate).ifPresent(intermediate -> {
      if (intermediate) {
        predicates.add(es -> {
          final FederationEntityMetadata federationEntityMetadata =
              EntityStatementClaims.getFederationEntityMetadata(es);
          return Objects.nonNull(federationEntityMetadata)
              && Objects.nonNull(federationEntityMetadata.getFederationFetchEndpointURI())
              && Objects.nonNull(federationEntityMetadata.getFederationListEndpointURI());
        });
      } else {
        predicates.add(es -> {
          final FederationEntityMetadata federationEntityMetadata =
              EntityStatementClaims.getFederationEntityMetadata(es);
          return Objects.isNull(federationEntityMetadata)
              || (Objects.isNull(federationEntityMetadata.getFederationFetchEndpointURI())
              && Objects.isNull(federationEntityMetadata.getFederationListEndpointURI()));
        });
      }
    });

    return predicates.stream().reduce((p) -> true, Predicate::and);
  }

  private static List<TrustMarkEntry> trustMarksOf(final SignedJWT es) {
    return Optional.ofNullable(EntityStatementClaims.getTrustMarks(es)).orElseGet(List::of);
  }
}
