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
package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.Nullable;
import se.digg.oidfed.common.exception.ServerErrorException;
import se.digg.oidfed.common.jwt.SignerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Signer for trust marks.
 *
 * @author Felix Hellman
 */
public class TrustMarkSigner {

  /**
   * Type for trust mark jwk.
   */
  public static final JOSEObjectType TRUST_MARK_JWT_TYPE = new JOSEObjectType("trust-mark+jwt");

  private static final SecureRandom rng = new SecureRandom();
  private final SignerFactory signerFactory;
  private final Clock clock;

  /**
   * Constructor.
   * @param signerFactory
   * @param clock
   */
  public TrustMarkSigner(final SignerFactory signerFactory, final Clock clock) {
    this.signerFactory = signerFactory;
    this.clock = clock;
  }


  /**
   * Signs a trust mark.
   * @param trustMarkIssuerProperties
   * @param trustMarkProperties
   * @param trustMarkSubject
   * @return signed jwt
   * @throws ServerErrorException
   * @throws ParseException
   * @throws JOSEException
   */
  public SignedJWT sign(
      final TrustMarkIssuerProperties trustMarkIssuerProperties,
      final TrustMarkIssuerProperties.TrustMarkProperties trustMarkProperties,
      final TrustMarkSubject trustMarkSubject) throws ServerErrorException, ParseException, JOSEException {
    // https://openid.net/specs/openid-federation-1_0.html#name-trust-mark-claims
    final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder()
        .issueTime(new Date(Instant.now(this.clock).toEpochMilli()))
        .jwtID(new BigInteger(128, rng).toString(16))
        .subject(trustMarkSubject.sub());


    claimsSetBuilder.expirationTime(
        this.calculateExp(trustMarkIssuerProperties.trustMarkValidityDuration(), trustMarkSubject.expires()));

    final Optional<EntityID> issuerEntityId = Optional.ofNullable(trustMarkIssuerProperties.issuerEntityId());

    if (issuerEntityId.isEmpty()) {
      throw new ServerErrorException("Issuer must be present");
    }

    issuerEntityId.ifPresent(entityID -> claimsSetBuilder.issuer(entityID.getValue()));
    claimsSetBuilder.claim("id", trustMarkProperties.trustMarkId().getTrustMarkId());

    trustMarkProperties.logoUri().ifPresent((value) -> claimsSetBuilder.claim("logo_uri", value));

    trustMarkProperties.refUri().ifPresent((value) -> claimsSetBuilder.claim("ref", value));

    trustMarkProperties.delegation()
        .ifPresent((value) -> claimsSetBuilder.claim("delegation", value.getDelegation()));

    final JWTClaimsSet claimsSet = claimsSetBuilder.build();
    return this.signerFactory.createSigner()
        .sign(TRUST_MARK_JWT_TYPE, claimsSet);
  }

  protected Date calculateExp(final Duration trustMarkDurationToLive, @Nullable final Instant subjectTimeToLive) {
    final Instant calculatedTrustMarkTTL = Instant.now(this.clock).plus(trustMarkDurationToLive);
    final Optional<Instant> subTimeToLive = Optional.ofNullable(subjectTimeToLive);

    if (subTimeToLive.isPresent() && calculatedTrustMarkTTL.isAfter(subTimeToLive.get())) {
      return new Date(subTimeToLive.get().toEpochMilli());
    }
    return new Date(calculatedTrustMarkTTL.toEpochMilli());
  }
}
