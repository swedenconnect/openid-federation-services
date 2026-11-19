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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.LazyResolve;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChainFactory;
import se.swedenconnect.oidf.resolver.tree.TrustChainSource;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds a trust chain at request time, from the leaf up through its authority hints to the Trust Anchor.
 *
 * <p>Of each superior, only its Entity Configuration and its one Subordinate Statement about the entity below it are
 * requested. With {@link LazyResolve#LOCAL} the leaf, and the issuers of its trust marks, must be hosted by this
 * service, otherwise no trust chain is found. With {@link LazyResolve#ALL} they may be outside this service.</p>
 *
 * <p>Only Leaf Entities have an {@code ec_location}. For a hosted leaf it is read from its entity record, and the
 * Entity Configuration is requested from it.</p>
 *
 * @author Felix Hellman
 */
@Slf4j
public class LazyTrustChainSource implements TrustChainSource {

  private final LazyResolve mode;
  private final FederationClient client;
  private final CompositeRecordSource source;

  /**
   * Constructor.
   *
   * @param mode     which leaves trust chains may be built for
   * @param client   to request the statements of the chain with
   * @param source   of the hosted entities
   */
  public LazyTrustChainSource(
      final LazyResolve mode,
      final FederationClient client,
      final CompositeRecordSource source) {

    this.mode = mode;
    this.client = client;
    this.source = source;
  }

  @Override
  public ResolverTrustChain trustChain(final ResolveRequest request) {
    final Optional<EntityRecord> record = this.source.getEntity(new NodeKey(request.subject()));
    if (this.mode == LazyResolve.LOCAL && record.isEmpty()) {
      log.debug("No trust chain for {}, it is not hosted", request.subject());
      return notFound();
    }
    final ScrapedEntity leaf = ScrapedEntity.builder()
        .entityID(new EntityID(request.subject()))
        .ecLocation(record.map(LazyTrustChainSource::ecLocation).orElse(null))
        .build();
    leaf.scrapeEntityConfiguration(this.client);
    if (this.mode == LazyResolve.LOCAL && !this.trustMarkIssuersHosted(leaf)) {
      log.debug("No trust chain for {}, a trust mark issuer is not hosted", request.subject());
      return notFound();
    }
    final List<ScrapedEntity> path = new ArrayList<>(List.of(leaf));
    final Set<String> onPath = new HashSet<>(Set.of(request.subject()));
    if (!request.subject().equals(request.trustAnchor())
        && !this.walkUp(leaf, request.trustAnchor(), path, onPath)) {
      log.debug("No trust chain for {}, no path to {}", request.subject(), request.trustAnchor());
      return notFound();
    }
    return ResolverTrustChainFactory.trustChainOf(new LinkedHashSet<>(path));
  }

  private static ResolverTrustChain notFound() {
    return ResolverTrustChainFactory.trustChainOf(new LinkedHashSet<>());
  }

  /**
   * Follows the authority hints of an entity to the Trust Anchor. A hint that leads nowhere is skipped.
   *
   * @param entity      the entity to continue from
   * @param trustAnchor the Trust Anchor to reach
   * @param path        the entities so far, leaf first; the superiors found are added
   * @param onPath      the entity identifiers on the path, to stop loops
   * @return true if the Trust Anchor was reached
   */
  private boolean walkUp(
      final ScrapedEntity entity, final String trustAnchor, final List<ScrapedEntity> path, final Set<String> onPath) {

    final String entityId = entity.getEntityID().getValue();
    final List<EntityID> hints =
        Optional.ofNullable(EntityStatementClaims.getAuthorityHints(entity.getEntityStatement())).orElseGet(List::of);
    for (final EntityID hint : hints) {
      if (onPath.contains(hint.getValue())) {
        continue;
      }
      final Optional<ScrapedEntity> superior = this.superior(hint, entityId);
      if (superior.isEmpty()) {
        continue;
      }
      path.add(superior.get());
      onPath.add(hint.getValue());
      if (hint.getValue().equals(trustAnchor) || this.walkUp(superior.get(), trustAnchor, path, onPath)) {
        return true;
      }
      path.removeLast();
      onPath.remove(hint.getValue());
    }
    return false;
  }

  /**
   * Builds a superior with its Entity Configuration and its one Subordinate Statement about the entity below.
   *
   * @param superiorId the superior
   * @param subject    the entity below it
   * @return the superior, or empty if it has no fetch endpoint or no statement about the subject
   */
  private Optional<ScrapedEntity> superior(final EntityID superiorId, final String subject) {
    try {
      final SignedJWT configuration = this.client.entityConfiguration(
          new FederationRequest<>(new EntityConfigurationRequest(superiorId, null)));
      final Map<String, Object> metadata =
          new EntityStatementWrapper(configuration).getFederationEntityMetadata().orElse(null);
      if (metadata == null || !metadata.containsKey("federation_fetch_endpoint")) {
        return Optional.empty();
      }
      final SignedJWT statement =
          this.client.fetch(new FederationRequest<>(new FetchRequest(subject), metadata));
      final ScrapedEntity superior = ScrapedEntity.builder()
          .entityID(superiorId)
          .entityStatement(configuration)
          .intermediate(new ScrapedIntermediate(new ConcurrentHashMap<>(Map.of(subject, statement))))
          .build();
      return Optional.of(superior);
    }
    catch (final RuntimeException e) {
      log.debug("Superior {} of {} skipped: {}", superiorId.getValue(), subject, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * The {@code ec_location} issued for a hosted entity: its {@code ec-location}, or the well-known location under its
   * {@code virtual-entity-id} when that differs from its entity identifier.
   *
   * @param record of the entity
   * @return the location, or null if the Entity Configuration is at its well-known location
   */
  private static String ecLocation(final EntityRecord record) {
    final String base = record.getPreferedEntityId().getValue();
    if (record.getEcLocation() != null) {
      return EcLocationValidator.resolve(record.getEcLocation(), base);
    }
    if (!base.equals(record.getEntityIdentifier().getValue())) {
      return EcLocationValidator.resolve("/.well-known/openid-federation", base);
    }
    return null;
  }

  private boolean hosts(final String entityId) {
    return this.source.getEntity(new NodeKey(entityId)).isPresent();
  }

  private boolean trustMarkIssuersHosted(final ScrapedEntity leaf) {
    for (final SignedJWT trustMark : new EntityStatementWrapper(leaf.getEntityStatement()).getTrustMarks()) {
      try {
        final String issuer = trustMark.getJWTClaimsSet().getIssuer();
        if (issuer != null && !this.hosts(issuer)) {
          return false;
        }
      }
      catch (final ParseException e) {
        return false;
      }
    }
    return true;
  }
}
