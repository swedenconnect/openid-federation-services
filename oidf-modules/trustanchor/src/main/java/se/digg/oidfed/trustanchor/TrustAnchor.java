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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.federation.EntityConfigurationRequest;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.federation.FederationRequest;
import se.digg.oidfed.common.entity.integration.federation.FetchRequest;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.entity.integration.registry.TrustAnchorProperties;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.common.exception.InvalidIssuerException;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.common.tree.NodeKey;

import java.util.List;
import java.util.Map;

/**
 * Implementation of trust anchor.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustAnchor {

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
  public TrustAnchor(
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

  /**
   * @param request to fetch entity statement for
   * @return entity statement
   * @throws InvalidIssuerException
   * @throws InvalidRequestException
   * @throws NotFoundException
   */
  public String fetchEntityStatement(final FetchRequest request)
      throws InvalidIssuerException, NotFoundException {
    final EntityRecord issuer = this.source.getEntity(
            new NodeKey(
                this.properties.getEntityId().getValue(),
                this.properties.getEntityId().getValue()
            )
        )
        .orElseThrow(
            () -> new InvalidIssuerException("Entity not found for:'%s'".formatted(this.properties.getEntityId()))
        );
    final EntityRecord subject = this.source.getEntity(new NodeKey(
            this.properties.getEntityId().getValue(),
            request.subject()
        ))
        .orElseThrow(
            () -> new NotFoundException("Entity not found for subject:'%s'".formatted(request.subject()))
        );

    if (!subject.getIssuer().equals(issuer.getSubject())) {
      throw new IllegalArgumentException("Subject is not listed under this issuer trustMarkIssuer:%s sub:%s"
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
    final List<String> subordinates = this.source
        .findSubordinates(this.properties.getEntityId().getValue()).stream()
        .filter(er -> !er.getSubject().equals(this.properties.getEntityId()))
        .map(ec -> ec.getSubject().getValue())
        .toList();

    if (!request.requiresFiltering()) {
      return subordinates;
    }

    final List<String> list = subordinates.stream().toList()
        .stream()
        .map(s -> new FederationRequest<>(new EntityConfigurationRequest(new EntityID(s)), Map.of(), true))
        .map(this.federationClient::entityConfiguration)
        .filter(request.toPredicate())
        .map(EntityStatement::getEntityID)
        .map(Identifier::getValue)
        .toList();
    if (list.isEmpty()) {
      throw new NotFoundException("No subordinates found");
    }
    return list;
  }

  /**
   * @return entity id of this trust anchor
   */
  public EntityID getEntityId() {
    return this.properties.getEntityId();
  }
}
