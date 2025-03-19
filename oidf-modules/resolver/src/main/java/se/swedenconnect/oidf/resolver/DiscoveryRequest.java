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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import se.swedenconnect.oidf.common.entity.tree.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Request object for discovery.
 *
 * @param trustAnchor
 * @param types
 * @param trustMarkIds
 * @author Felix Hellman
 */
public record DiscoveryRequest(String trustAnchor, List<String> types, List<String> trustMarkIds) {
  /**
   * @return this request as a search predicate
   */
  public BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>> asPredicate() {

    final List<BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>>> predicates = new ArrayList<>();

    predicates.add((a,s) -> a.getClaimsSet().isSelfStatement());

    if (Objects.isNull(this.trustAnchor)) {
      throw new IllegalArgumentException("Trust anchor parameter can not be null");
    }

    if (Objects.nonNull(this.types) && !this.types.isEmpty()) {
      predicates.add((a, s) -> this.types.stream()
          .anyMatch(type -> Objects.nonNull(a.getClaimsSet().getMetadata(new EntityType(type)))));
    }

    if (Objects.nonNull(this.trustMarkIds) && !this.trustMarkIds.isEmpty()) {
      predicates.add((a, s) -> {
        if (Objects.isNull(a.getClaimsSet().getTrustMarks())) {
          //We requested trust marks but there is none in this entity statement
          return false;
        }
        return a.getClaimsSet().getTrustMarks().stream()
            .anyMatch(tmp -> this.trustMarkIds.contains(tmp.getID().getValue()));
      });
    }

    return predicates.stream().reduce((a,b) -> true, BiPredicate::and);
  }
}
