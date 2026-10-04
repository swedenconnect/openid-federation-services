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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkOwner;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkValidator;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.InvalidRequestException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;

import java.time.Instant;
import java.util.HashMap;
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
      throws InvalidIssuerException, NotFoundException, InvalidRequestException {
    this.debugLogRequest(request);
    if (this.properties.getEntityIdentifier().getValue().equals(request.subject())) {
      // A statement about the issuer itself is never issued (Section 8.1.2)
      throw new InvalidRequestException("sub must not be the issuer itself");
    }
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

    final Instant now = Instant.now();
    final Map<String, JWKSet> issuerKeys = this.trustMarkIssuerKeys(subordinates);
    return subordinates.stream()
        .map(subordinate -> this.entityConfigurationOrNull(subordinate, now))
        .filter(Objects::nonNull)
        .filter(request.toPredicate(ec -> this.validTrustMarks(ec, issuerKeys, now)))
        .map(EntityStatementClaims::getEntityID)
        .map(EntityID::getValue)
        .toList();
  }

  /**
   * Fetches and validates the Entity Configuration of a subordinate. The signature is checked against the keys in
   * the Trust Anchor's own record of the subordinate.
   *
   * @param entity the subordinate
   * @param now the current time
   * @return the Entity Configuration, or null if it cannot be fetched or is not valid
   */
  private SignedJWT entityConfigurationOrNull(final TrustAnchorProperties.SubordinateListingProperty entity,
      final Instant now) {
    final EntityID entityID = entity.getEntityIdentifier();
    final SignedJWT entityConfiguration;
    try {
      entityConfiguration = this.federationClient.entityConfiguration(new FederationRequest<>(
          new EntityConfigurationRequest(entityID, entity.getEcLocation()),
          Map.of()));
    } catch (final Exception e) {
      log.warn("Skipping subordinate {} in filtered listing, entity configuration unavailable",
          entityID.getValue(), e);
      return null;
    }
    try {
      final JWTClaimsSet claims = entityConfiguration.getJWTClaimsSet();
      final String error;
      if (!entityID.getValue().equals(claims.getIssuer()) || !entityID.getValue().equals(claims.getSubject())) {
        error = "iss and sub are not the subordinate";
      }
      else if (entity.getJwks() == null) {
        error = "no jwks registered for the subordinate";
      }
      else if (claims.getIssueTime() == null
          || claims.getIssueTime().toInstant().isAfter(now.plus(TrustMarkValidator.CLOCK_SKEW))) {
        error = "iat is missing or in the future";
      }
      else if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(now)) {
        error = "exp is missing or has passed";
      }
      else {
        EntityStatementClaims.verifySignature(entityConfiguration, entity.getJwks());
        return entityConfiguration;
      }
      log.info("Skipping subordinate {} in filtered listing, invalid entity configuration: {}",
          entityID.getValue(), error);
    }
    catch (final java.text.ParseException | BadJOSEException | JOSEException e) {
      log.info("Skipping subordinate {} in filtered listing, invalid entity configuration: {}",
          entityID.getValue(), e.getMessage());
    }
    return null;
  }

  /**
   * Collects the keys of the entities whose trust marks can be validated: this Trust Anchor and its direct
   * subordinates, whose keys are in the Trust Anchor's own records.
   *
   * @param subordinates the subordinates of this Trust Anchor
   * @return keys by entity identifier
   */
  private Map<String, JWKSet> trustMarkIssuerKeys(
      final List<TrustAnchorProperties.SubordinateListingProperty> subordinates) {
    final Map<String, JWKSet> keys = new HashMap<>();
    subordinates.stream()
        .filter(subordinate -> subordinate.getJwks() != null)
        .forEach(subordinate -> keys.put(subordinate.getEntityIdentifier().getValue(), subordinate.getJwks()));
    final String trustAnchorId = this.properties.getEntityIdentifier().getValue();
    this.source.getEntity(new NodeKey(trustAnchorId))
        .map(EntityRecord::getJwks)
        .ifPresent(jwks -> keys.put(trustAnchorId, jwks));
    return keys;
  }

  /**
   * Gets the trust marks of an Entity Configuration that are valid (OpenID Federation 1.0, Section 7.3). Trust in the
   * issuer is established only for this Trust Anchor and its direct subordinates, whose keys are known from the
   * Trust Anchor's own records. Trust marks from other issuers are not counted.
   *
   * @param entityConfiguration the Entity Configuration of a subordinate
   * @param issuerKeys keys of the issuers that can be trusted
   * @param now the current time
   * @return the valid trust marks
   */
  private List<TrustMarkEntry> validTrustMarks(final SignedJWT entityConfiguration,
      final Map<String, JWKSet> issuerKeys, final Instant now) {
    final String subject = EntityStatementClaims.getEntityID(entityConfiguration).getValue();
    return Optional.ofNullable(EntityStatementClaims.getTrustMarks(entityConfiguration)).orElseGet(List::of).stream()
        .filter(entry -> {
          final String error = this.checkTrustMark(entry, subject, issuerKeys, now);
          if (error != null) {
            log.debug("Trust mark of type '{}' of {} not counted in listing: {}", entry.getID(), subject, error);
          }
          return error == null;
        })
        .toList();
  }

  private String checkTrustMark(final TrustMarkEntry entry, final String subject,
      final Map<String, JWKSet> issuerKeys, final Instant now) {
    final SignedJWT trustMark = entry.getTrustMark();
    try {
      final String claimsError = TrustMarkValidator.checkClaims(trustMark, subject, now);
      if (claimsError != null) {
        return claimsError;
      }
      final String type = EntityStatementClaims.getTrustMarkType(trustMark);
      if (!entry.getID().getValue().equals(type)) {
        return "trust_mark_type of the entry and the trust mark differ";
      }
      final String issuer = trustMark.getJWTClaimsSet().getIssuer();
      if (!this.isIssuerAllowed(type, issuer)) {
        return "issuer %s is not allowed by trust_mark_issuers".formatted(issuer);
      }
      final JWKSet keys = issuerKeys.get(issuer);
      if (keys == null) {
        return "issuer %s is not this Trust Anchor or one of its direct subordinates".formatted(issuer);
      }
      if (!TrustMarkValidator.verify(trustMark, keys)) {
        return "signature is not valid for the issuer keys";
      }
      final Optional<TrustMarkOwner> owner = Optional.ofNullable(this.properties.getTrustMarkOwners())
          .orElseGet(List::of).stream()
          .filter(o -> o.getTrustmarkIdentifier() != null && type.equals(o.getTrustmarkIdentifier().getValue()))
          .findFirst();
      if (owner.isPresent()) {
        return TrustMarkValidator.checkDelegation(trustMark, type, owner.get().getSub().getValue(),
            owner.get().getJwks(), now);
      }
      return null;
    }
    catch (final java.text.ParseException e) {
      return "failed to parse: " + e.getMessage();
    }
  }

  /**
   * Checks the issuer against the {@code trust_mark_issuers} of this Trust Anchor. When none are configured, any
   * issuer is allowed. A type with an empty list allows any issuer.
   *
   * @param type the trust mark type
   * @param issuer the trust mark issuer
   * @return true if the issuer may issue trust marks of the type
   */
  private boolean isIssuerAllowed(final String type, final String issuer) {
    final Map<EntityID, List<EntityID>> trustMarkIssuers = this.properties.getTrustMarkIssuers();
    if (trustMarkIssuers == null || trustMarkIssuers.isEmpty()) {
      return true;
    }
    final List<EntityID> issuers = trustMarkIssuers.get(new EntityID(type));
    return issuers != null && (issuers.isEmpty() || issuers.contains(new EntityID(issuer)));
  }

  @Override
  public EntityID getEntityId() {
    return this.properties.getEntityIdentifier();
  }

  private void debugLogRequest(final Object request) {
    log.debug("{} trust anchor module received request {}", this.properties, request);
  }
}
