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
package se.swedenconnect.oidf.resolver.trustmark;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusStore;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the trust mark status for all trust marks found in the federation tree and stores the results.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkStatusLoader {

  private final FederationClient federationClient;
  private final TrustMarkStatusStore store;
  private final String trustAnchorEntityId;

  /**
   * Constructor.
   *
   * @param federationClient    for calling trust mark status endpoints
   * @param store               for persisting status results
   * @param trustAnchorEntityId entity ID of the trust anchor
   */
  public TrustMarkStatusLoader(
      final FederationClient federationClient,
      final TrustMarkStatusStore store,
      final String trustAnchorEntityId) {
    this.federationClient = federationClient;
    this.store = store;
    this.trustAnchorEntityId = trustAnchorEntityId;
  }

  /**
   * Checks trust mark status for all entities in the tree and stores results.
   *
   * @param tree the entity statement tree to check
   */
  public void checkAll(final EntityStatementTree tree) {
    final Set<Tree.SearchResult<EntityStatement>> all = tree.getAll();

    // Find trust anchor self-statement to get trust_mark_issuers
    final Optional<EntityStatement> trustAnchorStatement = all.stream()
        .map(Tree.SearchResult::getData)
        .filter(es -> es.getEntityID().getValue().equals(this.trustAnchorEntityId))
        .filter(es -> es.getClaimsSet().isSelfStatement())
        .findFirst();

    if (trustAnchorStatement.isEmpty()) {
      log.debug("Trust anchor self-statement not found in tree, skipping trust mark status checks");
      return;
    }

    final JSONObject trustMarkIssuers = trustAnchorStatement.get()
        .getClaimsSet()
        .getJSONObjectClaim("trust_mark_issuers");

    if (trustMarkIssuers == null || trustMarkIssuers.isEmpty()) {
      log.debug("No trust_mark_issuers defined in trust anchor, skipping trust mark status checks");
      return;
    }

    final Set<String> relevantTypes = trustMarkIssuers.keySet();

    // Clear old results before populating fresh ones
    this.store.clear();

    // Build a map of entity ID -> federation_entity metadata for endpoint lookup
    final Map<String, JSONObject> entityFederationMetadata = new java.util.HashMap<>();
    all.forEach(result -> {
      final EntityStatement es = result.getData();
      final JSONObject fedMeta = es.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY);
      if (fedMeta != null) {
        entityFederationMetadata.put(es.getEntityID().getValue(), fedMeta);
      }
    });

    for (final Tree.SearchResult<EntityStatement> result : all) {
      final EntityStatement es = result.getData();
      final JSONArray trustMarks = es.getClaimsSet().getJSONArrayClaim("trust_marks");
      if (trustMarks == null) {
        continue;
      }

      final String subject = es.getClaimsSet().getSubject().getValue();

      for (final Object entry : trustMarks) {
        final JSONObject tmJson = (JSONObject) entry;
        final String trustMarkType;
        final String trustMarkJwt;
        try {
          final com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry tmEntry =
              com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry.parse(tmJson);
          trustMarkType = tmEntry.getID().getValue();
          trustMarkJwt = tmEntry.getTrustMark().serialize();
          final String issuer = tmEntry.getTrustMark().getJWTClaimsSet().getIssuer();

          if (!relevantTypes.contains(trustMarkType)) {
            continue;
          }

          final Map<String, Object> issuerMetadata = Optional.ofNullable(
                  entityFederationMetadata.get(issuer))
              .map(json -> (Map<String, Object>) new java.util.HashMap<>(json))
              .orElse(Map.of());

          try {
            final TrustMarkStatusResponse signedJWT = this.federationClient.trustMarkStatus(
                new FederationRequest<>(
                    new FederationTrustMarkStatusRequest(trustMarkJwt, issuer),
                    issuerMetadata,
                    false));
            this.store.setTrustMarkStatus(subject, trustMarkType, signedJWT);
          } catch (final Exception e) {
            log.warn("Failed to check trust mark status for subject={} type={}: {}",
                subject, trustMarkType, e.getMessage());
            this.store.setTrustMarkStatus(subject, trustMarkType, new TrustMarkStatusResponse(null, true));
          }
        } catch (final ParseException | java.text.ParseException e) {
          log.warn("Failed to parse trust mark entry, skipping: {}", e.getMessage());
        }
      }
    }
  }
}
