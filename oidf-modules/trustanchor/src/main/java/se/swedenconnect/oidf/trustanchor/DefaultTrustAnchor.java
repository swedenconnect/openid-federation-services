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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of trust anchor.
 *
 * @author Felix Hellman
 */
@Slf4j
public class DefaultTrustAnchor implements TrustAnchor {

  private final CompositeRecordSource source;

  private final TrustAnchorProperties properties;

  private final SubordinateStatementFactory factory;

  private final FederationClient federationClient;


  /**
   * Constructor.
   *
   * @param source           to use
   * @param properties       to use
   * @param factory          to constructor entity statements
   * @param federationClient to use for resolving entity configurations
   */
  public DefaultTrustAnchor(
      final CompositeRecordSource source,
      final TrustAnchorProperties properties,
      final SubordinateStatementFactory factory,
      final FederationClient federationClient
  ) {

    this.source = source;
    this.properties = properties;
    this.factory = factory;
    this.federationClient = federationClient;
  }

  @Override
  public String fetchEntityStatement(final FetchRequest request)
      throws InvalidIssuerException, NotFoundException {
    this.debugLogRequest(request);
    final EntityRecord issuer = this.source.getEntity(
            new NodeKey(this.properties.getEntityIdentifier().getValue())
        )
        .orElseThrow(
            () -> new InvalidIssuerException(
                "Entity not found for:'%s'".formatted(this.properties.getEntityIdentifier())
            )
        );

    final Optional<TrustAnchorProperties.SubordinateListingProperty> first = this.properties.getSubordinates().stream()
        .filter(p -> request.subject().equals(p.getEntityIdentifier().getValue()))
        .findFirst();

    if (first.isEmpty()) {
      throw new NotFoundException("No subordinates found");
    }

    final TrustAnchorProperties.SubordinateListingProperty subordinate = first.get();


    return this.factory
        .createEntityStatement(issuer, subordinate)
        .serialize();
  }

  @Override
  public List<String> subordinateListing(final SubordinateListingRequest request) throws FederationException {
    this.debugLogRequest(request);

    final List<TrustAnchorProperties.SubordinateListingProperty> subordinates = this.source
        .findSubordinates(this.properties.getEntityIdentifier().getValue()).stream()
        .toList();

    if (!request.requiresFiltering()) {
      return subordinates.stream().map(e -> e.getEntityIdentifier().getValue()).toList();
    }

    return subordinates.stream()
        .map(this::entityConfigurationOrNull)
        .filter(Objects::nonNull)
        .filter(request.toPredicate())
        .map(EntityStatementClaims::getEntityID)
        .map(EntityID::getValue)
        .toList();
  }

  private SignedJWT entityConfigurationOrNull(final TrustAnchorProperties.SubordinateListingProperty entity) {
    final EntityID entityID = entity.getEntityIdentifier();
    try {
      return this.federationClient.entityConfiguration(new FederationRequest<>(
          new EntityConfigurationRequest(entityID, entity.getEcLocation()),
          Map.of()));
    } catch (final Exception e) {
      log.warn("Skipping subordinate {} in filtered listing, entity configuration unavailable",
          entityID.getValue(), e);
      return null;
    }
  }

  @Override
  public EntityID getEntityId() {
    return this.properties.getEntityIdentifier();
  }

  private void debugLogRequest(final Object request) {
    log.debug("{} trust anchor module received request {}", this.properties, request);
  }
}
