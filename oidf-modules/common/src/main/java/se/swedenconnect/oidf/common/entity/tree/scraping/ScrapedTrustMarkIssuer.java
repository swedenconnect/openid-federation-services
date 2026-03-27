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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds scraped trust mark data for a trust mark issuer, keyed by trust mark type.
 *
 * @param trustMark map of trust mark type to scraped trust mark data
 * @author Felix Hellman
 */
public record ScrapedTrustMarkIssuer(Map<String, ScrapedTrustMark> trustMark) {

  /**
   * Looks up trust mark info for a given type and subject.
   *
   * @param trustMarkType    the trust mark type
   * @param trustMarkSubject the subject
   * @return the matching info, or empty if not found
   */
  public Optional<ScrapedTrustMarkInfo> trustMarkInfo(final String trustMarkType, final String trustMarkSubject) {
    return Optional.ofNullable(this.trustMark().get(trustMarkType))
        .flatMap(tm -> Optional.ofNullable(tm.subjects()))
        .flatMap(tms -> Optional.ofNullable(tms.get(trustMarkSubject)));
  }

  void addTrustMarkInfo(final ScrapedTrustMarkInfo info) {
    this.trustMark().computeIfAbsent(info.trustMarkType(), key -> {
      final ScrapedTrustMark addedTrustMark = new ScrapedTrustMark(info.trustMarkType(), new HashMap<>());
      addedTrustMark.subjects().put(info.trustMarkSubject(), info);
      return addedTrustMark;
    });
  }

  /**
   * Scrapes trust mark data for this issuer from the federation.
   *
   * @param client      federation client to use
   * @param metadata    metadata for the issuer endpoint
   * @param trustAnchor the trust anchor entity statement
   * @param entityID    the entity ID of this issuer
   */
  public void scrape(
      final FederationClient client,
      final Map<String, Object> metadata,
      final EntityStatementWrapper trustAnchor,
      final EntityID entityID) {
    if (metadata.containsKey("federation_trust_mark_list_endpoint")) {
      Optional.ofNullable(trustAnchor.getEntityStatement()
              .getClaimsSet()
              .getTrustMarksIssuers()
              .get(entityID))
          .ifPresent(tmi -> {
            //This entity is a valid trust mark issuer for this federation.
            tmi.forEach(tm -> {
              // For every trust mark get subjects
              final List<String> trustMarkSubjects =
                  client.trustMarkedListing(new FederationRequest<>(new TrustMarkListingRequest(tm.getValue(), null),
                      metadata));
              trustMarkSubjects.forEach(tms -> {
                // Trust Mark per subject
                final TrustMarkRequest trustMarkRequest =
                    new TrustMarkRequest(new EntityID(tms), entityID, new EntityID(tm.getValue()));
                final SignedJWT trustMark = client.trustMark(new FederationRequest<>(trustMarkRequest));
                // Status per subject
                final TrustMarkStatusResponse trustMarkStatus = client.trustMarkStatus(
                    new FederationRequest<>(new FederationTrustMarkStatusRequest(
                        trustMark.serialize(),
                        entityID.getValue())
                    )
                );
                final ScrapedTrustMarkInfo info = new ScrapedTrustMarkInfo(
                    entityID.getValue(), tm.getValue(), tms, trustMark, trustMarkStatus);
                this.addTrustMarkInfo(info);
              });
            });
          });
    }
  }
}

