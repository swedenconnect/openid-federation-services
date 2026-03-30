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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private final EntityID entityID;
  private final Instant scrapedAt = Instant.now();
  private String ecLocation;

  // Base info
  private EntityStatement entityStatement;
  private Map<String, TrustMarkStatusResponse> trustMarkStatuses = new HashMap<>();
  //Roles
  private ScrapedIntermediate intermediate;
  private ScrapedTrustMarkIssuer trustMarkIssuer;

  /**
   * Resolves the entity statement using the provided federation client.
   *
   * @param client      the federation client to use for resolution
   * @param trustAnchor entity configuration
   */
  public void scrape(final FederationClient client, final EntityStatementWrapper trustAnchor) {
    log.debug("Resolving entity {}", this.entityID);
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
    });
    wrapper.getFederationEntityMetadata()
        .ifPresent(metadata -> {
          if (metadata.containsKey("federation_trust_mark_list_endpoint") && trustAnchor != null) {
            final JSONObject trustMarkIssuers = trustAnchor.getEntityStatement()
                .getClaimsSet()
                .getJSONObjectClaim("trust_mark_issuers");
            if (trustMarkIssuers != null) {
              log.debug("Entity {} is trust mark issuer, resolving trust mark issuer information", this.entityID);
              this.trustMarkIssuer = new ScrapedTrustMarkIssuer(new HashMap<>());
              this.trustMarkIssuer.scrape(client, metadata, trustAnchor, this.entityID);
            }
          }
          if (metadata.containsKey("federation_list_endpoint")) {
            log.debug("Entity {} is intermediate, resolving subordinates", this.entityID);
            this.intermediate = new ScrapedIntermediate(new HashMap<>());
            this.intermediate.scrape(client, metadata);
          }
        });
  }

}
