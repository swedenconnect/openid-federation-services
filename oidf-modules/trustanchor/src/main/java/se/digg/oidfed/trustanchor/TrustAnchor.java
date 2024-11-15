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
package se.digg.oidfed.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.entity.EntityRegistry;
import se.digg.oidfed.common.module.Submodule;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementation of trust anchor.
 *
 * @author Felix Hellman
 */
public class TrustAnchor implements Submodule {

  private final EntityRegistry registry;

  private final TrustAnchorProperties properties;

  private final SubordinateStatementFactory factory;

  /**
   * Constructor.
   *
   * @param registry to use
   * @param properties to use
   * @param factory to constructor entity statements 
   */
  public TrustAnchor(final EntityRegistry registry, final TrustAnchorProperties properties,
      final SubordinateStatementFactory factory) {
    this.registry = registry;
    this.properties = properties;
    this.factory = factory;
  }

  /**
   * @param request to fetch entity statement for
   * @return entity statement
   */
  public String fetchEntityStatement(final EntityStatementRequest request) {
    final EntityProperties issuer = registry.getEntity(this.properties.getEntityId()).orElseThrow();
    final EntityProperties subject = registry.getEntity(new EntityID(request.subject())).orElseThrow();

    final Predicate<TrustAnchorProperties.SubordinateListingProperty> subordinatePredicate = sub -> sub
        .getEntityIdentifier()
        .equalsIgnoreCase(request.subject());

    final Optional<TrustAnchorProperties.SubordinateListingProperty> subordinateListing =
        this.properties.getSubordinateListing().stream().filter(subordinatePredicate).findFirst();

    if (subordinateListing.isEmpty()) {
      throw new IllegalArgumentException("Subject is not listed under this issuer iss:%s sub:%s"
          .formatted(issuer.getEntityIdentifier(), subject.getEntityIdentifier()));
    }

    return factory
        .createEntityStatement(issuer, subject, subordinateListing.get())
        .serialize();
  }

  /**
   * @param request to get subordinate listing for current module
   * @return listing of subordinates
   */
  public List<String> subordinateListing(final SubordinateListingRequest request) {
    return this.properties.getSubordinateListing()
        .stream()
        .map(TrustAnchorProperties.SubordinateListingProperty::getEntityIdentifier)
        .toList();
  }

  @Override
  public String getAlias() {
    return properties.getAlias();
  }
}
