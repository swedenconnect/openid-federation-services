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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * @param subject
 * @param trustAnchor
 * @param type
 * @param explain
 * @author Felix Hellman
 */
public record ResolveRequest(String subject, String trustAnchor, String type, Boolean explain) implements Serializable {
  /**
   * @return this request as a search predicate
   */
  public BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>> asPredicate() {
    final List<BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>>> predicates = new ArrayList<>();

    predicates.add((a,s) -> a.getEntityStatement().getClaimsSet().isSelfStatement());

    if (Objects.nonNull(this.subject)) {
      predicates.add((a, s) -> a.getEntityStatement().getClaimsSet().getSubject().getValue()
          .equalsIgnoreCase(this.subject));
    }
    if (Objects.nonNull(this.type)) {
      predicates.add((a, s) -> Objects.nonNull(
          a.getEntityStatement().getClaimsSet().getMetadata(new EntityType(this.type))));
    }

    return predicates.stream().reduce((a,b) -> true, BiPredicate::and);
  }

  /**
   * @param resolverEntity of the resolver
   * @return this request as a string key
   */
  public String toKey(final EntityID resolverEntity) {
    return "%s|%s|%s|%s".formatted(
        resolverEntity.getValue(),
        this.subject,
        this.trustAnchor,
        this.type
    );
  }

  /**
   * @param key to parse
   * @return key as request
   */
  public static ResolveRequest fromKey(final String key) {
    final String[] split = key.split("\\|");
    return new ResolveRequest(
        split[1],
        split[2],
        split[3],
        false
    );
  }
}

