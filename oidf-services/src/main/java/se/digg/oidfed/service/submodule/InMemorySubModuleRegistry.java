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
package se.digg.oidfed.service.submodule;

import se.digg.oidfed.common.module.Submodule;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.trustanchor.TrustAnchor;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In memory registry for holding submodules.
 *
 * @author Felix Hellman
 */
public class InMemorySubModuleRegistry implements ResolverModuleRepository {
  private final Map<String, Resolver> resolvers = new HashMap<>();
  private final Map<String, TrustAnchor> trustAnchors = new HashMap<>();
  private final Map<String, TrustMarkIssuer> trustMarkIssuers = new HashMap<>();

  /**
   * Takes a list of resolvers and registers them to the repository.
   * @param resolverList to register
   */
  public void registerResolvers(final List<Resolver> resolverList) {
    resolverList.forEach(r -> this.resolvers.put(r.getAlias(), r));
  }
  /**
   * Takes a list of resolvers and registers them to the repository.
   * @param trustMarkIssuerList to register
   */
  public void registerTrustMarkIssuer(final List<TrustMarkIssuer> trustMarkIssuerList) {
    trustMarkIssuerList.forEach(r -> this.trustMarkIssuers.put(r.getAlias(), r));
  }

  @Override
  public Optional<Resolver> getResolver(final String alias) {
    return Optional.ofNullable(resolvers.get(alias));
  }

  /* Add these when the respective module is added.
  public Optional<TrustAnchor> getTrustAnchor(final String alias) {
    return Optional.ofNullable(trustAnchors.get(alias));
  }
*/

 @Override
  public Optional<TrustMarkIssuer> getTrustMarkIssuer(final String alias) {
    return Optional.ofNullable(trustMarkIssuers.get(alias));
  }

}
