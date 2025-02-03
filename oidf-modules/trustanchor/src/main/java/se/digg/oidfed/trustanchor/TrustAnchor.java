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

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.common.exception.InvalidIssuerException;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.common.exception.UncheckedFederationException;
import se.digg.oidfed.common.module.Submodule;

import java.util.List;
import java.util.Objects;

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

  private final EntityConfigurationLoader loader;

  /**
   * Constructor.
   *
   * @param registry   to use
   * @param properties to use
   * @param factory    to constructor entity statements
   * @param loader     for loading entity configurations
   */
  public TrustAnchor(
      final EntityRecordRegistry registry,
      final TrustAnchorProperties properties,
      final SubordinateStatementFactory factory,
      final EntityConfigurationLoader loader) {

    this.registry = registry;
    this.properties = properties;
    this.factory = factory;
    this.loader = loader;
  }

  /**
   * @param request to fetch entity statement for
   * @return entity statement
   * @throws InvalidIssuerException
   * @throws InvalidRequestException
   * @throws NotFoundException
   */
  public String fetchEntityStatement(final EntityStatementRequest request)
      throws InvalidIssuerException, InvalidRequestException, NotFoundException {
    final EntityRecord issuer = this.registry.getEntity(this.properties.getEntityId())
        .orElseThrow(
            () -> new InvalidIssuerException("Entity not found for:'%s'".formatted(this.properties.getEntityId()))
        );
    final EntityRecord subject = this.registry.getEntity(new EntityID(request.subject()))
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
    final List<String> list = this.registry
        .find(ec -> ec.getIssuer().getValue().equalsIgnoreCase(this.properties.getEntityId().getValue()) &&
            !ec.getIssuer().getValue().equalsIgnoreCase(ec.getSubject().getValue())).stream()
        .map(ec -> ec.getSubject().getValue())
        .toList();

    if (!request.requiresFiltering()) {
      return list;
    }

    return this.loadEntityConfiguration(list).stream()
        .filter(request.toPredicate())
        .map(EntityStatement::getEntityID)
        .map(Identifier::getValue)
        .toList();
  }

  private List<EntityStatement> loadEntityConfiguration(final List<String> entityIds) throws FederationException {
    // Fetch entity statement internally and resolve entity configurations.
    try {
      return entityIds.stream()
          .map(EntityStatementRequest::new)
          .map(r -> {
            try {
              return this.fetchEntityStatement(r);
            } catch (final FederationException e) {
              throw new UncheckedFederationException(e);
            }
          })
          .map(es -> {
            try {
              return this.loader.load(EntityStatement.parse(es));
            } catch (final ParseException e) {
              log.warn("Skipping parse failed Entity statement %s".formatted(es), e);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .toList();
    } catch (final UncheckedFederationException e) {
      throw e.getCheckedException();
    }
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
