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
package se.swedenconnect.oidf.common.entity.entity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSourceProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Factory class for creating entity statements.
 *
 * @author Felix Hellman
 */
public class SigningEntityConfigurationFactory implements EntityConfigurationFactory {

  private final FederationClient federationClient;

  private final List<EntityConfigurationClaimCustomizer> customizers;

  /**
   * @param federationClient to supply eventual trust marks
   * @param customizers      to customize records with
   */
  public SigningEntityConfigurationFactory(
      final FederationClient federationClient,
      final List<EntityConfigurationClaimCustomizer> customizers) {

    this.federationClient = federationClient;
    this.customizers = customizers;
  }

  @Override
  public EntityStatement createEntityConfiguration(final EntityRecord record) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
      this.customizers.forEach(c -> c.customize(record, builder));
      builder.issuer(record.getEntityIdentifier().getValue());
      builder.subject(record.getEntityIdentifier().getValue());
      builder.issueTime(Date.from(Instant.now()));
      builder.expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
      builder.claim("metadata", record.getMetadata());
      builder.claim("authority_hints", record.getAuthorityHints());
      builder.claim("jwks", record.getJwks().toPublicJWKSet().toJSONObject());
      final List<TrustMarkSourceProperty> trustMarkSourceProperties = record.getTrustMarkSource();
      if (Objects.nonNull(trustMarkSourceProperties)) {
        final List<TrustMarkEntry> trustMarks = trustMarkSourceProperties.stream()
            .map(s -> new TrustMarkRequest(record.getEntityIdentifier(), s.issuer(), new EntityID(s.trustMarkId())))
            .map(request -> {
              final SignedJWT signedJWT =
                  this.federationClient.trustMark(new FederationRequest<>(request, Map.of(), true));
              return new TrustMarkEntry(request.trustMarkId(), signedJWT);
            })
            .toList();
        builder.claim("trust_marks", trustMarks.stream().map(TrustMarkEntry::toJSONObject).toList());
      }
      return EntityStatement.sign(new EntityStatementClaimsSet(builder.build()), record.getJwks().getKeys().getFirst());
    } catch (JOSEException | ParseException e) {
      throw new IllegalArgumentException("Failed to sign entity configuration", e);
    }
  }
}
