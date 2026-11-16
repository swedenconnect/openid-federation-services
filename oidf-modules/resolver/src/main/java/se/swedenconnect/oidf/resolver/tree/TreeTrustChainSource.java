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

import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;

/**
 * Reads trust chains from the resolver's tree, following the authority hints of the subject first.
 *
 * @author Felix Hellman
 */
public class TreeTrustChainSource implements TrustChainSource {

  private final EntityStatementTree tree;

  /**
   * Constructor.
   *
   * @param tree to read trust chains from
   */
  public TreeTrustChainSource(final EntityStatementTree tree) {
    this.tree = tree;
  }

  @Override
  public ResolverTrustChain trustChain(final ResolveRequest request) {
    return this.tree.getTrustChainViaAuthorityHints(request)
        .orElseGet(() -> this.tree.getTrustChain(request));
  }
}
