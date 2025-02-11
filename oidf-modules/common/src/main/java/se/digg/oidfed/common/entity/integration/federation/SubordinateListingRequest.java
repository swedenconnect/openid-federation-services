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
package se.digg.oidfed.common.entity.integration.federation;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;

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
 * @param trustMarkId
 * @param intermediate
 * @author Felix Hellman
 */
public record SubordinateListingRequest(String entityType, Boolean trustMarked, String trustMarkId,
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
    return Stream.of(this.entityType, this.trustMarkId, this.trustMarked, this.intermediate)
        .anyMatch(Objects::nonNull);
  }

  /**
   * Converts the request into an {@link EntityStatement} predicate
   * @return request as predicate
   */
  public Predicate<EntityStatement> toPredicate() {
    final List<Predicate<EntityStatement>> predicates = new ArrayList<>();

    Optional.ofNullable(this.entityType).ifPresent(type -> {
      predicates.add(es -> Objects.nonNull(es.getClaimsSet().getMetadata(new EntityType(type))));
    });

    Optional.ofNullable(this.trustMarkId).ifPresent(tmid -> {
      final Predicate<EntityStatement> predicate =
          es -> es.getClaimsSet().getTrustMarks().stream()
              .anyMatch(tme -> tme.getID().getValue().equals(tmid));
      predicates.add(predicate);
    });

    Optional.ofNullable(this.trustMarked).ifPresent(marked -> {
      if (marked) {
        predicates.add(es -> {
          return Objects.nonNull(es.getClaimsSet().getTrustMarks())
              && !es.getClaimsSet().getTrustMarks().isEmpty();
        });
      } else {
        predicates.add(es -> {
          return Objects.isNull(es.getClaimsSet().getTrustMarks()) || es.getClaimsSet().getTrustMarks().isEmpty();
        });
      }
    });

    Optional.ofNullable(this.intermediate).ifPresent(intermediate -> {
      if (intermediate) {
        predicates.add(es -> {
          final FederationEntityMetadata federationEntityMetadata = es.getClaimsSet().getFederationEntityMetadata();
          return Objects.nonNull(federationEntityMetadata.getFederationFetchEndpointURI())
              && Objects.nonNull(federationEntityMetadata.getFederationListEndpointURI());
        });
      } else {
        predicates.add(es -> {
          final FederationEntityMetadata federationEntityMetadata = es.getClaimsSet().getFederationEntityMetadata();
          return Objects.isNull(federationEntityMetadata.getFederationFetchEndpointURI())
              && Objects.isNull(federationEntityMetadata.getFederationListEndpointURI());
        });
      }
    });

    return predicates.stream().reduce((p) -> true, Predicate::and);
  }
}
