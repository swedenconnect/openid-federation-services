/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import se.digg.oidfed.common.tree.Node;

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
public record ResolverRequest(String subject, String trustAnchor, String type) {
  /**
   * @return this request as a search predicate
   */
  public BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>> asPredicate() {
    final List<BiPredicate<EntityStatement, Node.NodeSearchContext<EntityStatement>>> predicates = new ArrayList<>();

    predicates.add((a,s) -> a.getClaimsSet().isSelfStatement());

    if (Objects.nonNull(subject)) {
      predicates.add((a,s) -> a.getClaimsSet().getSubject().getValue().equalsIgnoreCase(subject));
    }
    if (Objects.nonNull(type)) {
      predicates.add((a,s) -> Objects.nonNull(a.getClaimsSet().getMetadata(new EntityType(type))));
    }

    return predicates.stream().reduce((a,b) -> true, BiPredicate::and);
  }
}

