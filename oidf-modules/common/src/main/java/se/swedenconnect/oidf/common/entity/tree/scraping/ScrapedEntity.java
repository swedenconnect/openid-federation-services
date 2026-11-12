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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wrapper for an entity statement in the resolver tree.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ScrapedEntity {
  private static final String STATUS_ENDPOINT = "federation_trust_mark_status_endpoint";

  private final EntityID entityID;
  private final Instant scrapedAt = Instant.now();
  private String ecLocation;

  // Base info
  private SignedJWT entityStatement;
  /** Trust mark status responses, keyed by the serialized trust mark JWT. */
  @Builder.Default
  private Map<String, TrustMarkStatusResponse> trustMarkStatuses = new HashMap<>();
  //Roles
  private ScrapedIntermediate intermediate;
  /** Time of the last successful fetch of the Entity Configuration, or {@code null} if not known. */
  private Instant lastSeen;
  /** The latest failed fetch of the Entity Configuration, or {@code null} if the latest fetch succeeded. */
  private FetchError lastError;

  /**
   * Scrapes the entity: its Entity Configuration, the status of its trust marks and, for an intermediate, its
   * subordinate listing and Subordinate Statements. Each part is handled on its own. When fetching a part fails, the
   * same part of the previous data is used while it is still valid:
   * <ul>
   *   <li>the Entity Configuration of an intermediate or trust anchor always, and that of a leaf while its
   *   {@code exp} has not passed, unless its {@code kid} is not in its own {@code jwks},</li>
   *   <li>the status of a trust mark while the trust mark has not expired,</li>
   *   <li>the subordinate listing and Subordinate Statements as described by
   *   {@link ScrapedIntermediate#scrape(String, FederationClient, Map, ScrapedIntermediate, ScrapeFailureListener)}.
   *   </li>
   * </ul>
   *
   * @param client   the federation client to use
   * @param previous the data of the entity in the previous snapshot, or {@code null}
   * @param listener receives the failed fetches
   * @return {@code false} if the entity is to be left out, since its Entity Configuration or subordinate listing
   *     could not be fetched and there is no valid previous value
   */
  public boolean scrape(final FederationClient client, final ScrapedEntity previous,
      final ScrapeFailureListener listener) {
    log.debug("Resolving entity {}", this.entityID);
    try {
      this.fetchEntityConfiguration(client);
    }
    catch (final Exception e) {
      log.trace("Failed to fetch Entity Configuration of {}", this.entityID, e);
      final boolean previousUsed = !(e instanceof WrongJwkKidException) && previous != null
          && previous.entityStatement != null
          && (isIntermediate(previous.entityStatement) || isValid(previous.entityStatement));
      listener.failed(ScrapeFailureListener.Part.ENTITY_CONFIGURATION, this.entityID.getValue(), reason(e),
          previousUsed);
      if (!previousUsed) {
        return false;
      }
      this.entityStatement = previous.entityStatement;
      this.lastSeen = previous.lastSeen;
      this.lastError = FetchError.of(e);
    }
    this.scrapeTrustMarkStatuses(client, previous, listener);
    final Optional<Map<String, Object>> metadata = listingMetadata(this.entityStatement);
    if (metadata.isPresent()) {
      log.debug("Entity {} is intermediate, resolving subordinates", this.entityID);
      this.intermediate = ScrapedIntermediate.scrape(this.entityID.getValue(), client, metadata.get(),
          previous != null ? previous.intermediate : null, listener);
      return this.intermediate != null;
    }
    return true;
  }

  /**
   * Fetches the Entity Configuration and checks that its {@code kid} is in its own {@code jwks}. An expired Entity
   * Configuration is kept and marked with {@link FetchError#expired()}, it is left to the resolver to reject it.
   *
   * @param client the federation client to use
   * @throws WrongJwkKidException if the {@code kid} is not in the {@code jwks}
   */
  private void fetchEntityConfiguration(final FederationClient client) {
    final SignedJWT entityConfiguration =
        client.entityConfiguration(
            new FederationRequest<>(new EntityConfigurationRequest(this.entityID, this.ecLocation))
        );
    final String headerKid = entityConfiguration.getHeader().getKeyID();
    final JWKSet jwkSet = EntityStatementClaims.getJWKSet(entityConfiguration);
    if (headerKid != null && jwkSet != null) {
      final Set<String> jwksKids = jwkSet.getKeys().stream()
          .map(JWK::getKeyID)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      if (!jwksKids.isEmpty() && !jwksKids.contains(headerKid)) {
        throw new WrongJwkKidException(this.entityID.getValue(), headerKid, jwksKids);
      }
    }
    this.entityStatement = entityConfiguration;
    this.lastSeen = Instant.now();
    final Instant exp = expirationTime(entityConfiguration);
    this.lastError = exp != null && !exp.isAfter(Instant.now()) ? FetchError.expired() : null;
  }

  /**
   * Gives the {@code federation_entity} metadata of an Entity Configuration if it has a subordinate listing endpoint.
   *
   * @param entityConfiguration the Entity Configuration
   * @return the metadata, or empty if the entity is not an intermediate or trust anchor
   */
  private static Optional<Map<String, Object>> listingMetadata(final SignedJWT entityConfiguration) {
    return new EntityStatementWrapper(entityConfiguration).getFederationEntityMetadata()
        .filter(m -> m.containsKey("federation_list_endpoint"));
  }

  /**
   * Tells whether an Entity Configuration is that of an intermediate or trust anchor.
   *
   * @param entityConfiguration the Entity Configuration
   * @return true if it has a subordinate listing endpoint
   */
  private static boolean isIntermediate(final SignedJWT entityConfiguration) {
    try {
      return listingMetadata(entityConfiguration).isPresent();
    }
    catch (final Exception e) {
      return false;
    }
  }

  /**
   * Tells whether the {@code exp} of a JWT has not passed. A JWT without {@code exp} is not valid.
   *
   * @param jwt the JWT
   * @return true if the JWT has not expired
   */
  private static boolean isValid(final SignedJWT jwt) {
    final Instant exp = expirationTime(jwt);
    return exp != null && exp.isAfter(Instant.now());
  }

  /**
   * Fetches the status of each trust mark of the Entity Configuration. When a status cannot be fetched, the previous
   * status of the same trust mark is used while the trust mark has not expired.
   *
   * @param client   the federation client to use
   * @param previous the data of the entity in the previous snapshot, or {@code null}
   * @param listener receives the failed fetches
   */
  private void scrapeTrustMarkStatuses(final FederationClient client, final ScrapedEntity previous,
      final ScrapeFailureListener listener) {
    final List<SignedJWT> trustMarks = new EntityStatementWrapper(this.entityStatement).getTrustMarks();
    final Map<String, Optional<Map<String, Object>>> issuerMetadataCache = new HashMap<>();
    trustMarks.forEach(trustMark -> {
      final String trustMarkType = EntityStatementClaims.getTrustMarkType(trustMark);
      final String issuer;
      try {
        issuer = trustMark.getJWTClaimsSet().getIssuer();
      } catch (final ParseException e) {
        log.info("Ignoring trust mark of {} with invalid claims: {}", this.entityID, e.getMessage());
        return;
      }
      if (trustMarkType == null || issuer == null) {
        log.info("Ignoring trust mark of {} without trust_mark_type or issuer", this.entityID);
        return;
      }
      log.debug("Resolving trust mark status for {} of type {}", this.entityID, trustMarkType);
      final Optional<Map<String, Object>> issuerMetadata =
          issuerMetadataCache.computeIfAbsent(issuer, i -> issuerMetadata(client, i));
      TrustMarkStatusResponse trustMarkStatus;
      if (issuerMetadata.isEmpty()) {
        trustMarkStatus = new TrustMarkStatusResponse(null, true);
      }
      else if (!issuerMetadata.get().containsKey(STATUS_ENDPOINT)) {
        log.debug("Trust mark issuer {} has no {}", issuer, STATUS_ENDPOINT);
        trustMarkStatus = TrustMarkStatusResponse.noStatusEndpoint();
      }
      else {
        trustMarkStatus = client.trustMarkStatus(new FederationRequest<>(
            new FederationTrustMarkStatusRequest(trustMark.serialize(), issuer), issuerMetadata.get()));
      }
      if (trustMarkStatus.isError()) {
        final TrustMarkStatusResponse old = previous != null && previous.trustMarkStatuses != null
            ? previous.trustMarkStatuses.get(trustMark.serialize())
            : null;
        final Instant exp = expirationTime(trustMark);
        final boolean previousUsed = old != null && !old.isError() && old.getSignedJWT() != null
            && (exp == null || exp.isAfter(Instant.now()));
        listener.failed(ScrapeFailureListener.Part.TRUST_MARK_STATUS, this.entityID.getValue(),
            "status of trust mark of type '%s' could not be fetched from '%s'".formatted(trustMarkType, issuer),
            previousUsed);
        if (previousUsed) {
          trustMarkStatus = old;
        }
      }
      else if (!trustMarkStatus.isNoStatusEndpoint()) {
        trustMarkStatus.setLastChecked(Instant.now());
      }
      this.trustMarkStatuses.put(trustMark.serialize(), trustMarkStatus);
    });
  }

  /**
   * Gives the {@code exp} of a JWT.
   *
   * @param jwt the JWT
   * @return the expiration time, or {@code null} if the JWT has none or its claims cannot be parsed
   */
  static Instant expirationTime(final SignedJWT jwt) {
    try {
      return Optional.ofNullable(jwt.getJWTClaimsSet().getExpirationTime()).map(Date::toInstant).orElse(null);
    }
    catch (final ParseException e) {
      return null;
    }
  }

  /**
   * Describes the cause of a failed fetch.
   *
   * @param e the cause
   * @return description holding the exception class and message
   */
  static String reason(final Exception e) {
    return "%s: %s".formatted(e.getClass().getSimpleName(), e.getMessage());
  }

  /**
   * Fetches the {@code federation_entity} metadata of a trust mark issuer from its Entity Configuration.
   *
   * @param client the federation client
   * @param issuer the trust mark issuer
   * @return the metadata, an empty map if the issuer has none, or empty if the Entity Configuration could not be
   *     fetched
   */
  private static Optional<Map<String, Object>> issuerMetadata(final FederationClient client, final String issuer) {
    try {
      final SignedJWT issuerConfiguration = client.entityConfiguration(
          new FederationRequest<>(new EntityConfigurationRequest(new EntityID(issuer), null)));
      return Optional.of(new EntityStatementWrapper(issuerConfiguration).getFederationEntityMetadata()
          .orElseGet(Map::of));
    }
    catch (final Exception e) {
      log.info("Failed to fetch Entity Configuration of trust mark issuer {}: {}", issuer, e.getMessage());
      return Optional.empty();
    }
  }
}
