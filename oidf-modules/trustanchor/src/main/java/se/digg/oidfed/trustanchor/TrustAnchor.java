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
package se.digg.oidfed.trustanchor;

import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.digg.oidfed.common.entity.integration.federation.EntityConfigurationRequest;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.federation.FederationRequest;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.federation.FetchRequest;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.entity.integration.registry.TrustAnchorProperties;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.common.exception.InvalidIssuerException;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.common.module.Submodule;

import java.util.List;
import java.util.Map;

/**
 * Implementation of trust anchor.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustAnchor implements Submodule {

  private final EntityRecordRegistry registry;

  private final TrustAnchorProperties properties;

  private final SubordinateStatementFactory factory;

  private final FederationClient federationClient;


  /**
   * Constructor.
   *
   * @param registry   to use
   * @param properties to use
   * @param factory    to constructor entity statements
   * @param federationClient to use for resolving entity configurations
   */
  public TrustAnchor(
      final EntityRecordRegistry registry,
      final TrustAnchorProperties properties,
      final SubordinateStatementFactory factory,
      final FederationClient federationClient
  ) {

    this.registry = registry;
    this.properties = properties;
    this.factory = factory;
    this.federationClient = federationClient;
  }

  /**
   * @param request to fetch entity statement for
   * @return entity statement
   * @throws InvalidIssuerException
   * @throws InvalidRequestException
   * @throws NotFoundException
   */
  public String fetchEntityStatement(final FetchRequest request)
      throws InvalidIssuerException, NotFoundException {
    final EntityRecord issuer = this.registry.getEntity(this.properties.getEntityId())
        .orElseThrow(
            () -> new InvalidIssuerException("Entity not found for:'%s'".formatted(this.properties.getEntityId()))
        );
    final EntityRecord subject = this.registry.getSubordinateRecord(new EntityID(request.subject()))
        .orElseThrow(
            () -> new NotFoundException("Entity not found for subject:'%s'".formatted(request.subject()))
        );

    if (!subject.getIssuer().equals(issuer.getSubject())) {
      throw new IllegalArgumentException("Subject is not listed under this issuer iss:%s sub:%s"
          .formatted(subject.getIssuer(), issuer.getSubject()));
    }

    return this.factory
        .createEntityStatement(issuer, subject)
        .serialize();
  }

  /**
   * @param request to get subordinate listing for current module
   * @return listing of subordinates
   * @throws FederationException when loading entity configurations fails
   */
  public List<String> subordinateListing(final SubordinateListingRequest request) throws FederationException {
    final List<String> subordinates = this.registry
        .findSubordinates(this.properties.getEntityId().getValue()).stream()
        .filter(er -> !er.getSubject().equals(this.properties.getEntityId()))
        .map(ec -> ec.getSubject().getValue())
        .toList();

    if (!request.requiresFiltering()) {
      return subordinates;
    }

    return subordinates.stream().toList()
        .stream()
        .map(s -> new FederationRequest<>(new EntityConfigurationRequest(new EntityID(s)), Map.of(), true))
        .map(this.federationClient::entityConfiguration)
        .filter(request.toPredicate())
        .map(EntityStatement::getEntityID)
        .map(Identifier::getValue)
        .toList();
  }

  @Override
  public String getAlias() {
    return this.properties.getAlias();
  }

  @Override
  public List<EntityID> getEntityIds() {
    return List.of(this.properties.getEntityId());
  }
}
