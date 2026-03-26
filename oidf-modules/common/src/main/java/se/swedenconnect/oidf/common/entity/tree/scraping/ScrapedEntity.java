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
package se.swedenconnect.oidf.common.entity.tree;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wrapper for an entity statement in the resolver tree.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ScrapedEntity {
  private final EntityID entityID;
  private final Instant scrapedAt = Instant.now();
  private String ecLocation;

  // Base info
  private EntityStatement entityStatement;
  private Map<String, TrustMarkStatusResponse> trustMarkStatuses;
  // Intermediate
  private Map<String, SignedJWT> subordinates;
  //Trust Mark Issuer
  private ScrapedTrustMarkIssuer trustMarkIssuer;


  //Trust mark Name to Subject mapping


  /**
   * Resolves the entity statement using the provided federation client.
   *
   * @param client the federation client to use for resolution
   * @param trustAnchor entity configuration
   */
  public void resolve(final FederationClient client, final EntityStatement trustAnchor) {
    this.entityStatement =
        client.entityConfiguration(
            new FederationRequest<>(new EntityConfigurationRequest(this.entityID, this.ecLocation))
        );
    final EntityStatementWrapper wrapper = new EntityStatementWrapper(this.entityStatement.getSignedStatement());
    final List<SignedJWT> trustMarks = wrapper.getTrustMarks();
    trustMarks.forEach(trustMark -> {
      try {
        final TrustMarkStatusResponse trustMarkStatus = client.trustMarkStatus(
            new FederationRequest<>(
                new FederationTrustMarkStatusRequest(trustMark.serialize(), trustMark.getJWTClaimsSet().getIssuer())
            )
        );
        final String trustMarkType = trustMark.getJWTClaimsSet().getStringClaim("trust_mark_type");
        this.trustMarkStatuses.put(trustMarkType, trustMarkStatus);
      } catch (final ParseException e) {
        throw new RuntimeException(e);
      }
      //Entity is federation entity
      wrapper.getFederationEntityMetadata()
          .ifPresent(metadata -> {
            this.handleSubordinateListing(client, metadata);
            this.handleTrustMarkListing(client, metadata, trustAnchor);
            //We can not resolve all resolver responses here since they are still being calculated.
          });
    });

  }

  //TODO move to Scaped Trust Mark Issuer
  private void handleTrustMarkListing(
      final FederationClient client,
      final Map<String, Object> metadata,
      final EntityStatement trustAnchor) {
    if (metadata.containsKey("federation_trust_mark_list_endpoint")) {
      this.trustMarkIssuer = new ScrapedTrustMarkIssuer(new HashMap<>());
      Optional.ofNullable(trustAnchor.getClaimsSet().getTrustMarksIssuers().get(this.entityID))
          .ifPresent(tmi -> {
            //This entity is a valid trust mark issuer for this federation.
            tmi.forEach(tm -> {
              // For every trust mark get subjects
              final List<String> trustMarkSubjects =
                  client.trustMarkedListing(new FederationRequest<>(new TrustMarkListingRequest(tm.getValue(), null),
                      metadata));
              trustMarkSubjects.forEach(tms -> {
                // Trust Mark per subject
                final SignedJWT trustMark = client.trustMark(new FederationRequest<>(new TrustMarkRequest(new EntityID(tms), this.entityID,
                    new EntityID(tm.getValue()))));
                // Status per subject
                final TrustMarkStatusResponse trustMarkStatus = client.trustMarkStatus(
                    new FederationRequest<>(new FederationTrustMarkStatusRequest(
                        trustMark.serialize(),
                        this.entityID.getValue())
                    )
                );
                final ScrapedTrustMarkInfo info = new ScrapedTrustMarkInfo(this.entityID.getValue(), tm.getValue(), tms, trustMark, trustMarkStatus);
                this.trustMarkIssuer.addTrustMarkInfo(info);
              });
            });
          });
    }
  }

  //TODO move to Scraped Intermediate
  private void handleSubordinateListing(final FederationClient client, final Map<String, Object> metadata) {
    if (metadata.containsKey("federation_list_endpoint")) {
      final List<String> subordinates = client.subordinateListing(
          new FederationRequest<>(SubordinateListingRequest.requestAll()));
      subordinates.forEach(sub -> {
        final EntityStatement fetch = client.fetch(new FederationRequest<>(new FetchRequest(sub), metadata));
        this.subordinates.put(sub, fetch.getSignedStatement());
      });
    }
  }
}
