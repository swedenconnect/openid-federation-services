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
 * Source of the trust chains a resolver validates.
 *
 * @author Felix Hellman
 */
@FunctionalInterface
public interface TrustChainSource {

  /**
   * @param request the resolve request
   * @return the trust chain from the subject to the Trust Anchor of the request
   * @throws RuntimeException if no trust chain can be found
   */
  ResolverTrustChain trustChain(final ResolveRequest request);
}
