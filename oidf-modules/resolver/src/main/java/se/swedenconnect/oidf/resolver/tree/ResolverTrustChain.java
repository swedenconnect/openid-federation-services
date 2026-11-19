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
import lombok.Getter;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.List;
import java.util.Set;

/**
 * Holds a resolved trust chain and the scraped leaf entity. A chain whose links could not all be built is kept
 * together with the reasons, so that the rest of the chain can still be validated and every problem reported.
 *
 * @author Felix Hellman
 */
@Getter
public class ResolverTrustChain {
  private final Set<SignedJWT> trustChain;
  private final ScrapedEntity leafEntity;
  private final List<String> buildErrors;

  /**
   * Constructor for a chain that was built without errors.
   *
   * @param trustChain the statements of the chain, leaf first
   * @param leafEntity the scraped leaf entity
   */
  public ResolverTrustChain(final Set<SignedJWT> trustChain, final ScrapedEntity leafEntity) {
    this(trustChain, leafEntity, List.of());
  }

  /**
   * Constructor.
   *
   * @param trustChain the statements of the chain, leaf first
   * @param leafEntity the scraped leaf entity
   * @param buildErrors the reasons the chain could not be fully built, empty if it was
   */
  public ResolverTrustChain(final Set<SignedJWT> trustChain, final ScrapedEntity leafEntity,
      final List<String> buildErrors) {
    this.trustChain = trustChain;
    this.leafEntity = leafEntity;
    this.buildErrors = List.copyOf(buildErrors);
  }

  /**
   * @return true if every link of the chain could be built
   */
  public boolean isComplete() {
    return this.buildErrors.isEmpty();
  }
}
