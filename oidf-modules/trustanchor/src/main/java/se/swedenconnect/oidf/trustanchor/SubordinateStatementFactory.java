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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
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

  private static final SecureRandom RNG = new SecureRandom();

  private final SignerFactory signerFactory;

  /**
   * Constructor.
   *
   * @param signerFactory for signing subordinate statements
   */
  public SubordinateStatementFactory(final SignerFactory signerFactory) {
    this.signerFactory = signerFactory;
  }

  /**
   * Creates a signed entity statement from the issuer.
   *
   * @param issuer  to create the statement from
   * @param subordinate to create statement for
   * @return a signed entity statement
   */
  public SignedJWT createEntityStatement(final EntityRecord issuer,
                                         final TrustAnchorProperties.SubordinateListingProperty subordinate) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

      Optional.ofNullable(subordinate.getConstraints())
          .ifPresent(constraint -> {
            builder.claim("constraints", constraint.toJson());
          });

      Optional.ofNullable(subordinate.getCrit()).ifPresent(
          crit -> builder.claim("crit", crit)
      );

      Optional.ofNullable(subordinate.getMetadataPolicyCrit()).ifPresent(
          metadataPolicyCrit -> builder.claim("metadata_policy_crit", metadataPolicyCrit)
      );

      final String resolvedEcLocation = resolveEcLocation(subordinate);
      if (resolvedEcLocation != null) {
        builder.claim("ec_location", resolvedEcLocation);
      }

      Optional.ofNullable(subordinate.getMetadataPolicyCrit()).ifPresent(
          metadataPolicyCrit -> builder.claim("metadata_policy_crit", metadataPolicyCrit)
      );

      Optional.ofNullable(subordinate.getPolicy())
          .flatMap(policy -> Optional.ofNullable(policy.getPolicy()))
          .ifPresent(policyRecord -> builder.claim("metadata_policy", policyRecord));

      builder.claim("jwks", subordinate.getJwks().toJSONObject(true));

      final JWTClaimsSet jwtClaimsSet = builder
          .issueTime(Date.from(Instant.now()))
          .expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
          .jwtID(new BigInteger(128, RNG).toString(16))
          .issuer(issuer.getEntityIdentifier().getValue())
          .subject(subordinate.getEntityIdentifier().getValue())
          .build();

      return this.signerFactory
          .createSigner(issuer)
          .sign(new JOSEObjectType("entity-statement+jwt"), jwtClaimsSet);
    } catch (final Exception e) {
      throw new EntityStatementSignException("Failed to sign entity statement", e);
    }
  }

  private static String resolveEcLocation(final TrustAnchorProperties.SubordinateListingProperty subordinate) {
    final String entityId = subordinate.getEntityIdentifier().getValue();
    final String virtualEntityId = subordinate.getVirtualEntityId() != null
        ? subordinate.getVirtualEntityId().getValue()
        : null;

    final String ecLocation = subordinate.getEcLocation();

    if (ecLocation != null) {
      if (ecLocation.startsWith("http://") || ecLocation.startsWith("https://")) {
        return ecLocation;
      }
      if (ecLocation.startsWith("/") && virtualEntityId != null) {
        return virtualEntityId + ecLocation;
      }
      if (ecLocation.startsWith("/")) {
        return entityId + ecLocation;
      }
    }

    // No ec_location set — if virtual entity ID differs from entity ID the subordinate
    // is hosted under a different domain, so we must tell others where to find it.
    if (virtualEntityId != null && !virtualEntityId.equals(entityId)) {
      return virtualEntityId + "/.well-known/openid-federation";
    }

    return null;
  }
}
