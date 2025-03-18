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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

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

  private final SignerFactory factory;

  /**
   * Constructor.
   *
   * @param factory for signing hosted records
   */
  public SubordinateStatementFactory(final SignerFactory factory) {
    this.factory = factory;
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

      Optional.ofNullable(subject.getPolicyRecord()).ifPresent(policyRecord -> builder.claim("metadata_policy",
          policyRecord.getPolicy()));

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
