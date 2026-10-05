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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Wrapper for an entity statement in the resolver tree.
 *
 * @author Felix Hellman
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
  /**
   * Time when scraping of the entity started to fail, or {@code null} if the data comes from the latest scrape.
   */
  private Instant scrapeFailedAt;

  /**
   * Creates a copy of this entity for use when a new scrape of the entity has failed. The copy keeps the data of
   * this entity and is marked with the time when scraping started to fail.
   *
   * @param failedAt the time when scraping of the entity started to fail
   * @return a copy of this entity
   */
  public ScrapedEntity copyForFailedScrape(final Instant failedAt) {
    return ScrapedEntity.builder()
        .entityID(this.entityID)
        .ecLocation(this.ecLocation)
        .entityStatement(this.entityStatement)
        .trustMarkStatuses(this.trustMarkStatuses)
        .intermediate(this.intermediate)
        .scrapeFailedAt(failedAt)
        .build();
  }

  /**
   * Resolves the entity statement using the provided federation client.
   *
   * @param client      the federation client to use for resolution
   */
  public void scrape(final FederationClient client) {
    log.debug("Resolving entity {}", this.entityID);
    final SignedJWT entityConfiguration =
        client.entityConfiguration(
            new FederationRequest<>(new EntityConfigurationRequest(this.entityID, this.ecLocation))
        );
    this.entityStatement = entityConfiguration;
    final String headerKid = this.entityStatement.getHeader().getKeyID();
    final JWKSet jwkSet = EntityStatementClaims.getJWKSet(this.entityStatement);
    if (headerKid != null && jwkSet != null) {
      final Set<String> jwksKids = jwkSet.getKeys().stream()
          .map(JWK::getKeyID)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      if (!jwksKids.isEmpty() && !jwksKids.contains(headerKid)) {
        throw new WrongJwkKidException(this.entityID.getValue(), headerKid, jwksKids);
      }
    }
    final EntityStatementWrapper wrapper = new EntityStatementWrapper(this.entityStatement);
    final List<SignedJWT> trustMarks = wrapper.getTrustMarks();
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
      final TrustMarkStatusResponse trustMarkStatus;
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
      this.trustMarkStatuses.put(trustMark.serialize(), trustMarkStatus);
    });
    wrapper.getFederationEntityMetadata()
        .ifPresent(metadata -> {
          if (metadata.containsKey("federation_list_endpoint")) {
            log.debug("Entity {} is intermediate, resolving subordinates", this.entityID);
            this.intermediate = new ScrapedIntermediate(new ConcurrentHashMap<>());
            this.intermediate.scrape(client, metadata);
          }
        });
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
