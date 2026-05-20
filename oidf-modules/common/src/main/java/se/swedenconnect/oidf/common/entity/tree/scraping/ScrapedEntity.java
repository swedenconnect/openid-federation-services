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

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
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
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
  @Builder.Default
  private Map<String, TrustMarkStatusResponse> trustMarkStatuses = new HashMap<>();
  //Roles
  private ScrapedIntermediate intermediate;

  /**
   * Resolves the entity statement using the provided federation client.
   *
   * @param client      the federation client to use for resolution
   */
  public void scrape(final FederationClient client) {
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
}
