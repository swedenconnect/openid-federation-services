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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.HostedRecord;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
   * @throws NotFoundException
   */
  public String fetchEntityStatement(final FetchRequest request)
      throws InvalidIssuerException, NotFoundException {
    this.debugLogRequest(request);
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

  private void debugLogRequest(final Object request) {
    log.debug("{} trust anchor module received request {}", this.properties, request);
  }

  /**
   * @param request to get subordinate listing for current module
   * @return listing of subordinates
   * @throws FederationException when loading entity configurations fails
   */
  public List<String> subordinateListing(final SubordinateListingRequest request) throws FederationException {
    this.debugLogRequest(request);
    final List<EntityRecord> subordinates = this.source
        .findSubordinates(this.properties.getEntityId().getValue()).stream()
        .filter(er -> !er.getSubject().equals(this.properties.getEntityId()))
        .toList();

    if (!request.requiresFiltering()) {
      return subordinates.stream().map(e -> e.getSubject().getValue()).toList();
    }

    final List<String> list = subordinates.stream().toList()
        .stream()
        .map(entity -> new FederationRequest<>(new EntityConfigurationRequest(new EntityID(entity.getSubject().getValue())),
            Optional.ofNullable(entity.getHostedRecord())
                .map(HostedRecord::getMetadata)
                .orElse(Map.of()),
            true))
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
