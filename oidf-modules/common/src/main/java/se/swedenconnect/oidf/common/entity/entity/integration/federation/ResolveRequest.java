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
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.Node;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * A resolve request (OpenID Federation 1.0, Section 8.3.1).
 *
 * @param subject the entity to resolve
 * @param trustAnchor the trust anchor to resolve with
 * @param types the requested entity types, null or empty for all types
 * @param explain true if an explanation of validation errors is requested
 * @author Felix Hellman
 */
public record ResolveRequest(String subject, String trustAnchor, List<String> types, Boolean explain)
    implements Serializable {

  /**
   * Gets the search predicate that finds the subject of this request. Entity types are not part of the search; the
   * resolved metadata is filtered on the requested types instead.
   *
   * @return this request as a search predicate
   */
  public BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>> asPredicate() {
    final List<BiPredicate<ScrapedEntity, Node.NodeSearchContext<ScrapedEntity>>> predicates = new ArrayList<>();

    predicates.add((a, s) -> a != null);
    predicates.add((a, s) -> a.getEntityStatement() != null);

    predicates.add((a,s) -> EntityStatementClaims.isSelfStatement(a.getEntityStatement()));

    if (Objects.nonNull(this.subject)) {
      predicates.add((a, s) -> EntityStatementClaims.claims(a.getEntityStatement()).getSubject()
          .equalsIgnoreCase(this.subject));
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
        this.types == null ? "" : String.join(",", this.types.stream().sorted().toList())
    );
  }

  /**
   * @param key to parse
   * @return key as request
   */
  public static ResolveRequest fromKey(final String key) {
    final String[] split = key.split("\\|", -1);
    final String types = split.length > 3 ? split[3] : "";
    return new ResolveRequest(
        split[1],
        split[2],
        types.isEmpty() || "null".equals(types) ? null : List.of(types.split(",")),
        false
    );
  }
}
