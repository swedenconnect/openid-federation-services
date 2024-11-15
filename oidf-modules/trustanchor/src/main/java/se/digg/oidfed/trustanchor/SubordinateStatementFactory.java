/*
 * Copyright 2024 Sweden Connect
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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.entity.PolicyRegistry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Entity statement factory for trust anchor.
 *
 * @author Felix Hellman
 */
public class SubordinateStatementFactory {

  private final PolicyRegistry registry;

  /**
   * Constructor.
   * @param registry of policies
   */
  public SubordinateStatementFactory(final PolicyRegistry registry) {
    this.registry = registry;
  }

  /**
   * Creates a signed entity statement from the issuer.
   * @param issuer to create the statement from
   * @param subject to create thte statement for
   * @param subordinateListing containing customization information for the statement, e.g. policy name
   * @return a signed entity statement
   */
  public SignedJWT createEntityStatement(final EntityProperties issuer, final EntityProperties subject,
      final TrustAnchorProperties.SubordinateListingProperty subordinateListing) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

      registry.getPolicy(subordinateListing.getPolicy())
          .ifPresent(policy -> builder.claim("metadata_policy", policy));

      final JWTClaimsSet jwtClaimsSet = builder
          .issueTime(Date.from(Instant.now()))
          .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
          .issuer(issuer.getEntityIdentifier())
          .subject(subject.getEntityIdentifier())
          .claim("jwks", subject.getJwks().toJSONObject())
          .build();

      final EntityStatement entityStatement =
          EntityStatement.sign(new EntityStatementClaimsSet(jwtClaimsSet), issuer.getSignKey());
      return entityStatement.getSignedStatement();
    }
    catch (final Exception e) {
      throw new EntityStatementSignException("Failed to sign entity statement", e);
    }
  }
}
