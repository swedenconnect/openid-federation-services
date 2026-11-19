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
package se.swedenconnect.oidf.resolver.tree;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * Factory for {@link ResolverTrustChain}.
 *
 * @author Felix Hellman
 */
public final class ResolverTrustChainFactory {

  private ResolverTrustChainFactory() {
  }

  /**
   * Builds the trust chain for a path of entities.
   *
   * @param entities the path, leaf first
   * @return the trust chain, holding the reasons for every link that could not be built: a superior that is not among
   *     the authority hints of its subordinate, that has no intermediate role, or that holds no subordinate statement
   *     about it. The remaining links are still built, so that the chain can be validated and all problems reported.
   */
  public static ResolverTrustChain trustChainOf(final SequencedSet<ScrapedEntity> entities) {
    //1. Use request to find path to the entity
    //2. Initial chain structure should be
    // leaf --> subordinateStatement --> (node --> subordinateStatement [repeated]) --> root
    //3. Remove all nodes in the chain that is not leaf or root
    final List<ScrapedEntity> entityList = new ArrayList<>(entities);
    final LinkedHashSet<SignedJWT> chain = new LinkedHashSet<>();
    final List<String> buildErrors = new ArrayList<>();

    if (entityList.isEmpty()) {
      return new ResolverTrustChain(chain, null);
    }

    // Add leaf entity configuration
    chain.add(entityList.getFirst().getEntityStatement());

    // For each consecutive (child, parent) pair, add the parent's subordinate statement about
    // the child. Intermediate self-configurations are omitted per step 3.
    for (int i = 0; i < entityList.size() - 1; i++) {
      final ScrapedEntity child = entityList.get(i);
      final ScrapedEntity parent = entityList.get(i + 1);

      // The superior must be among the authority hints of the entity (Section 3.2, step 6)
      final String parentId = EntityStatementClaims.getEntityID(parent.getEntityStatement()).getValue();
      final List<EntityID> hints = EntityStatementClaims.getAuthorityHints(child.getEntityStatement());
      if (hints == null || hints.stream().noneMatch(hint -> hint.getValue().equals(parentId))) {
        buildErrors.add("Entity %s does not list %s in its authority_hints"
            .formatted(child.getEntityID().getValue(), parentId));
      }

      final ScrapedIntermediate parentIntermediate = parent.getIntermediate();
      if (parentIntermediate == null) {
        buildErrors.add("Entity %s has no intermediate role but appears as parent in trust chain"
            .formatted(parent.getEntityID().getValue()));
        continue;
      }

      final SignedJWT subJWT = parentIntermediate.subordinates().get(child.getEntityID().getValue());
      if (subJWT == null) {
        buildErrors.add("Entity %s has no subordinate statement for %s"
            .formatted(parent.getEntityID().getValue(), child.getEntityID().getValue()));
        continue;
      }

      chain.add(subJWT);
    }

    // Add root (trust anchor) entity configuration
    if (entityList.size() > 1) {
      chain.add(entityList.getLast().getEntityStatement());
    }

    return new ResolverTrustChain(chain, entityList.getFirst(), buildErrors);
  }
}
