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
import se.digg.oidfed.common.tree.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * @param subject
 * @param trustAnchor
 * @param type
 * @author Felix Hellman
 */
public record ResolveRequest(String subject, String trustAnchor, String type) implements Serializable {
  /**
   * @return this request as a search predicate
   */
  public BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>> asPredicate() {
    final List<BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>>> predicates = new ArrayList<>();

    predicates.add((a,s) -> a.getClaimsSet().isSelfStatement());

    if (Objects.nonNull(this.subject)) {
      predicates.add((a,s) -> a.getClaimsSet().getSubject().getValue().equalsIgnoreCase(this.subject));
    }
    if (Objects.nonNull(this.type)) {
      predicates.add((a,s) -> Objects.nonNull(a.getClaimsSet().getMetadata(new EntityType(this.type))));
    }

    return predicates.stream().reduce((a,b) -> true, BiPredicate::and);
  }

  /**
   * @return this request as a string key
   */
  public String toKey() {
    return "%s:%s:%s".formatted(this.subject, this.trustAnchor, this.type);
  }
}

