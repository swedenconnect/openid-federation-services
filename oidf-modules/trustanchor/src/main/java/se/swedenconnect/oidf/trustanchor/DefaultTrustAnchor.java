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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.exception.UnsupportedParameterException;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TrustAnchor}, used for both Trust Anchors and Intermediate Entities. Subordinate
 * Statements and subordinate listings are created from the records of the entity.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
public class DefaultTrustAnchor implements TrustAnchor {

  /** The source of entity records and trust mark subjects. */
  private final CompositeRecordSource source;

  /** The record of this Trust Anchor, holding its subordinates. */
  private final TrustAnchorProperties properties;

  /** Factory for creating Subordinate Statements. */
  private final SubordinateStatementFactory factory;

  /**
   * Constructor.
   *
   * @param source the source of entity records and trust mark subjects
   * @param properties the record of this Trust Anchor
   * @param factory the factory for creating Subordinate Statements
   */
  public DefaultTrustAnchor(
      final CompositeRecordSource source,
      final TrustAnchorProperties properties,
      final SubordinateStatementFactory factory) {
    this.source = source;
    this.properties = properties;
    this.factory = factory;
  }

  /**
   * Creates a Subordinate Statement about a subordinate of this Trust Anchor (OpenID Federation 1.0, Section 8.1).
   *
   * @param request the fetch request
   * @return the signed Subordinate Statement
   * @throws InvalidRequestException if the subject is this Trust Anchor itself
   * @throws InvalidIssuerException if there is no entity record for this Trust Anchor
   * @throws NotFoundException if the subject is not a subordinate of this Trust Anchor
   */
  @Override
  public String fetchEntityStatement(final FetchRequest request)
      throws InvalidIssuerException, NotFoundException, InvalidRequestException {
    this.debugLogRequest(request);
    final String issuerId = this.properties.getEntityIdentifier().getValue();
    if (issuerId.equals(request.subject())) {
      // A statement about the issuer itself is never issued (Section 8.1.2)
      throw new InvalidRequestException("sub must not be the issuer itself");
    }
    final EntityRecord issuer = this.source.getEntity(new NodeKey(issuerId))
        .orElseThrow(() -> new InvalidIssuerException("Entity not found for:'%s'".formatted(issuerId)));
    final TrustAnchorProperties.SubordinateListingProperty subordinate = this.subordinates().stream()
        .filter(p -> request.subject().equals(p.getEntityIdentifier().getValue()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No subordinates found"));
    return this.factory.createEntityStatement(issuer, subordinate).serialize();
  }

  /**
   * Lists the subordinates of this Trust Anchor (OpenID Federation 1.0, Section 8.2). {@code entity_type} and
   * {@code intermediate} use the subordinate records. {@code trust_marked} and {@code trust_mark_type} use the trust
   * mark subjects of this entity, which must also be a Trust Mark Issuer.
   *
   * @param request the listing request
   * @return the entity identifiers of the matching subordinates
   * @throws UnsupportedParameterException if a trust mark filter is used and this entity is not a Trust Mark Issuer
   * @throws InvalidRequestException if {@code trust_mark_type} is a type this entity does not issue
   */
  @Override
  public List<String> subordinateListing(final SubordinateListingRequest request) throws FederationException {
    this.debugLogRequest(request);

    Predicate<TrustAnchorProperties.SubordinateListingProperty> filter = subordinate -> true;
    if (request.entityType() != null && !request.entityType().isEmpty()) {
      filter = filter.and(subordinate -> Optional.ofNullable(subordinate.getEntityTypes()).orElseGet(List::of)
          .stream()
          .anyMatch(request.entityType()::contains));
    }
    // trust_marked and intermediate only filter when true (Section 8.2.1)
    if (Boolean.TRUE.equals(request.intermediate())) {
      filter = filter.and(TrustAnchorProperties.SubordinateListingProperty::isIntermediate);
    }
    if (request.trustMarkType() != null || Boolean.TRUE.equals(request.trustMarked())) {
      final Set<String> trustMarked = this.trustMarkedSubjects(request.trustMarkType());
      filter = filter.and(subordinate -> trustMarked.contains(subordinate.getEntityIdentifier().getValue()));
    }
    return this.subordinates().stream()
        .filter(filter)
        .map(subordinate -> subordinate.getEntityIdentifier().getValue())
        .toList();
  }

  /**
   * Gets the subjects that hold a valid trust mark issued by this entity, which must also be a Trust Mark Issuer.
   *
   * @param trustMarkType the trust mark type to include, or {@code null} for all types this entity issues
   * @return the entity identifiers of the subjects
   * @throws UnsupportedParameterException if this entity is not a Trust Mark Issuer
   * @throws InvalidRequestException if this entity does not issue the given trust mark type
   */
  private Set<String> trustMarkedSubjects(final String trustMarkType)
      throws UnsupportedParameterException, InvalidRequestException {
    final TrustMarkIssuerProperties trustMarkIssuer = this.source.getTrustMarkIssuerProperties().stream()
        .filter(tmi -> this.properties.getEntityIdentifier().equals(tmi.entityIdentifier()))
        .findFirst()
        .orElseThrow(() -> new UnsupportedParameterException("%s is only supported by a Trust Mark Issuer"
            .formatted(trustMarkType != null ? "trust_mark_type" : "trust_marked")));
    final List<TrustMarkType> types = Optional.ofNullable(trustMarkIssuer.trustMarks()).orElseGet(List::of).stream()
        .map(TrustMarkProperties::getTrustMarkType)
        .filter(type -> trustMarkType == null || trustMarkType.equals(type.getTrustMarkType()))
        .toList();
    if (trustMarkType != null && types.isEmpty()) {
      throw new InvalidRequestException("trust_mark_type '%s' is not issued by this entity".formatted(trustMarkType));
    }
    final Instant now = Instant.now();
    return types.stream()
        .flatMap(type -> this.source.getTrustMarkSubjects(trustMarkIssuer.entityIdentifier(), type).stream())
        .filter(subject -> subject.isValid(now))
        .map(TrustMarkSubjectProperty::sub)
        .collect(Collectors.toSet());
  }

  /**
   * Gets the subordinates of this Trust Anchor. Fetch and listing use the same list, so that every listed subordinate
   * can be fetched. An entity listed more than once is only included the first time.
   *
   * @return the subordinates
   */
  private List<TrustAnchorProperties.SubordinateListingProperty> subordinates() {
    final Map<String, TrustAnchorProperties.SubordinateListingProperty> unique = new LinkedHashMap<>();
    Optional.ofNullable(this.properties.getSubordinates()).orElseGet(List::of)
        .forEach(subordinate -> unique.putIfAbsent(subordinate.getEntityIdentifier().getValue(), subordinate));
    return List.copyOf(unique.values());
  }

  /**
   * Gets the entity identifier of this Trust Anchor.
   *
   * @return the entity identifier
   */
  @Override
  public EntityID getEntityId() {
    return this.properties.getEntityIdentifier();
  }

  /**
   * Logs a received request at debug level.
   *
   * @param request the request
   */
  private void debugLogRequest(final Object request) {
    log.debug("{} trust anchor module received request {}", this.properties, request);
  }
}
