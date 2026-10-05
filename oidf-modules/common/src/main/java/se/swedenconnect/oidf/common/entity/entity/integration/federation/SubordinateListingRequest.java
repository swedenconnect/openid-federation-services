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
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param entityType
 * @param trustMarked
 * @param trustMarkType
 * @param intermediate
 * @author Felix Hellman
 */
public record SubordinateListingRequest(List<String> entityType, Boolean trustMarked, String trustMarkType,
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
    if (Objects.nonNull(this.entityType) && !this.entityType.isEmpty()) {
      return true;
    }
    // trust_marked and intermediate only filter when true (Section 8.2.1)
    return Objects.nonNull(this.trustMarkType)
        || Boolean.TRUE.equals(this.trustMarked)
        || Boolean.TRUE.equals(this.intermediate);
  }

  /**
   * Converts the request into a {@link SignedJWT} predicate. The trust mark filters use every trust mark in the
   * {@code trust_marks} claim, without validating them.
   *
   * @return request as predicate
   */
  public Predicate<SignedJWT> toPredicate() {
    return this.toPredicate(SubordinateListingRequest::trustMarksOf);
  }

  /**
   * Converts the request into a {@link SignedJWT} predicate, where the trust mark filters only count the trust marks
   * returned by {@code validTrustMarks}.
   *
   * @param validTrustMarks gives the valid trust marks of an Entity Configuration
   * @return request as predicate
   */
  public Predicate<SignedJWT> toPredicate(final Function<SignedJWT, List<TrustMarkEntry>> validTrustMarks) {
    final List<Predicate<SignedJWT>> predicates = new ArrayList<>();

    if (Objects.nonNull(this.entityType) && !this.entityType.isEmpty()) {
      predicates.add(es -> this.entityType.stream()
          .anyMatch(type -> Objects.nonNull(EntityStatementClaims.getMetadata(es, new EntityType(type)))));
    }

    Optional.ofNullable(this.trustMarkType).ifPresent(tmid -> {
      final Predicate<SignedJWT> predicate =
          es -> validTrustMarks.apply(es).stream()
              .anyMatch(tme -> tme.getID().getValue().equals(tmid));
      predicates.add(predicate);
    });

    // trust_marked and intermediate only filter when true; false means no filtering (Section 8.2.1)
    if (Boolean.TRUE.equals(this.trustMarked)) {
      predicates.add(es -> !validTrustMarks.apply(es).isEmpty());
    }

    if (Boolean.TRUE.equals(this.intermediate)) {
      // An entity with subordinates must have a fetch endpoint, a list endpoint is optional (Section 8.1)
      predicates.add(es -> {
        final FederationEntityMetadata federationEntityMetadata =
            EntityStatementClaims.getFederationEntityMetadata(es);
        return Objects.nonNull(federationEntityMetadata)
            && Objects.nonNull(federationEntityMetadata.getFederationFetchEndpointURI());
      });
    }

    return predicates.stream().reduce((p) -> true, Predicate::and);
  }

  private static List<TrustMarkEntry> trustMarksOf(final SignedJWT es) {
    return Optional.ofNullable(EntityStatementClaims.getTrustMarks(es)).orElseGet(List::of);
  }
}
