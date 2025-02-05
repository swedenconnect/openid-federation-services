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
package se.digg.oidfed.trustanchor;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.RefreshAheadRecordRegistrySource;
import se.digg.oidfed.common.jwt.SignerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Entity statement factory for trust anchor.
 *
 * @author Felix Hellman
 */
public class SubordinateStatementFactory {

  private final RefreshAheadRecordRegistrySource source;

  private final SignerFactory factory;

  private final String baseUri;

  /**
   * Constructor.
   *
   * @param source  of policies
   * @param factory for signing hosted records
   * @param baseUri for hosted records
   */
  public SubordinateStatementFactory(
      final RefreshAheadRecordRegistrySource source,
      final SignerFactory factory,
      final String baseUri) {
    this.source = source;
    this.factory = factory;
    this.baseUri = baseUri;
  }

  /**
   * Creates a signed entity statement from the issuer.
   *
   * @param issuer  to create the statement from
   * @param subject to create thte statement for
   * @return a signed entity statement
   */
  public SignedJWT createEntityStatement(final EntityRecord issuer, final EntityRecord subject) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

      Optional.ofNullable(subject.getPolicyRecordId()).flatMap(this.source::getPolicy)
          .ifPresent(policy -> builder.claim("metadata_policy", policy.getPolicy()));

      Optional.ofNullable(subject.getOverrideConfigurationLocation())
          .map(location -> {
            if (location.startsWith("/")) {
              return "%s%s/.well-known/openid-federation".formatted(this.baseUri, location);
            }
            return location;
          })
          .ifPresent(location -> builder.claim("subject_entity_configuration_location", location));

      Optional.ofNullable(subject.getJwks()).map(JWKSet::toJSONObject)
          .ifPresentOrElse(jwks -> builder.claim("jwks", jwks)
              , () -> {
                builder.claim("jwks", issuer.getJwks().toJSONObject());
              });


      final JWTClaimsSet jwtClaimsSet = builder
          .issueTime(Date.from(Instant.now()))
          .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
          .issuer(issuer.getSubject().getValue())
          .subject(subject.getSubject().getValue())
          .build();

      final EntityStatement entityStatement =
          EntityStatement.sign(new EntityStatementClaimsSet(jwtClaimsSet), this.factory.getSignKey());
      return entityStatement.getSignedStatement();
    } catch (final Exception e) {
      throw new EntityStatementSignException("Failed to sign entity statement", e);
    }
  }
}
