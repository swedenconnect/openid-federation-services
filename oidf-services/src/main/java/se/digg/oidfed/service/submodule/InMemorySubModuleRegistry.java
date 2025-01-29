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
package se.digg.oidfed.service.submodule;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.context.event.EventListener;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.trustanchor.TrustAnchor;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory registry for holding submodules.
 *
 * @author Felix Hellman
 */
public class InMemorySubModuleRegistry
    implements ResolverModuleRepository, TrustAnchorRepository, TrustMarkIssuerRepository {
  private final Map<String, Resolver> resolvers = new ConcurrentHashMap<>();
  private final Map<String, TrustAnchor> trustAnchors = new ConcurrentHashMap<>();
  private final Map<String, TrustMarkIssuer> trustMarkIssuers = new ConcurrentHashMap<>();

  /**
   * Takes a list of resolvers and registers them to the repository.
   *
   * @param resolverList to register
   */
  public void registerResolvers(final List<Resolver> resolverList) {
    resolverList.forEach(r -> this.resolvers.put(r.getAlias(), r));
  }

  /**
   * Takes a list of trust mark issuers and registers them to the repository.
   *
   * @param trustMarkIssuerList to register
   */
  public void registerTrustMarkIssuer(final List<TrustMarkIssuer> trustMarkIssuerList) {
    trustMarkIssuerList.forEach(r -> this.trustMarkIssuers.put(r.getAlias(), r));
  }

  /**
   * Takes a list of trustAnchors and registers them to the repository.
   *
   * @param trustAnchors to register
   */
  public void registerTrustAnchor(final List<TrustAnchor> trustAnchors) {
    trustAnchors.forEach(r -> this.trustAnchors.put(r.getAlias(), r));
  }

  @Override
  public Optional<Resolver> getResolver(final String alias) {
    return Optional.ofNullable(this.resolvers.get(alias));
  }

  @Override
  public Optional<TrustAnchor> getTrustAnchor(final String alias) {
    return Optional.ofNullable(this.trustAnchors.get(alias));
  }

  @Override
  public Optional<TrustMarkIssuer> getTrustMarkIssuer(final String alias) {
    return Optional.ofNullable(this.trustMarkIssuers.get(alias));
  }

  /**
   * @return entity id of all modules.
   */
  public List<EntityID> getAllEntityIds() {
    final ArrayList<EntityID> entityIDS = new ArrayList<>();
    entityIDS.addAll(this.resolvers.values().stream().flatMap(r -> r.getEntityIds().stream()).toList());
    entityIDS.addAll(this.trustAnchors.values().stream().flatMap(r -> r.getEntityIds().stream()).toList());
    entityIDS.addAll(this.trustMarkIssuers.values().stream().flatMap(r -> r.getEntityIds().stream()).toList());
    return entityIDS;
  }
}
