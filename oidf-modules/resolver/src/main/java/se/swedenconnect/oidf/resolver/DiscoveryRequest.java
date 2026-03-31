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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Request object for discovery.
 *
 * @param trustAnchor
 * @param types
 * @param trustMarkTypes
 * @author Felix Hellman
 */
public record DiscoveryRequest(String trustAnchor, List<String> types, List<String> trustMarkTypes) {
  /**
   * @return this request as a search predicate
   */
  public BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>> asPredicate() {

    final List<BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>>> predicates = new ArrayList<>();

    predicates.add((a, s) -> a.getEntityStatement().getClaimsSet().isSelfStatement());

    if (Objects.isNull(this.trustAnchor)) {
      throw new IllegalArgumentException("Trust anchor parameter can not be null");
    }

    if (Objects.nonNull(this.types) && !this.types.isEmpty()) {
      predicates.add((a, s) -> this.types.stream()
          .anyMatch(type -> Objects.nonNull(a.getEntityStatement().getClaimsSet().getMetadata(new EntityType(type)))));
    }

    if (Objects.nonNull(this.trustMarkTypes) && !this.trustMarkTypes.isEmpty()) {
      predicates.add((a, s) -> {
        if (Objects.isNull(a.getEntityStatement().getClaimsSet().getTrustMarks())) {
          //We requested trust marks but there is none in this entity statement
          return false;
        }
        return a.getEntityStatement().getClaimsSet().getTrustMarks().stream()
            .anyMatch(tmp -> this.trustMarkTypes.contains(tmp.getID().getValue()));
      });
    }

    return predicates.stream().reduce((a, b) -> true, BiPredicate::and);
  }
}
