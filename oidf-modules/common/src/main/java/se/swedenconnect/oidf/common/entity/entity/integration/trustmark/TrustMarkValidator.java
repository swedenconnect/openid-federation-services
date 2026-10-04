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
package se.swedenconnect.oidf.common.entity.entity.integration.trustmark;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.security.Key;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Checks of a trust mark that do not depend on how trust in the issuer is established (OpenID Federation 1.0,
 * Sections 7.2.2 and 7.3). The caller resolves the keys of the trust mark issuer and the trust mark owner.
 *
 * @author Martin Lindström
 */
@Slf4j
public final class TrustMarkValidator {

  /** The {@code typ} of a trust mark. */
  public static final String TRUST_MARK_JWT_TYPE = "trust-mark+jwt";

  /** Allowed clock skew when checking {@code iat}. */
  public static final Duration CLOCK_SKEW = Duration.ofSeconds(15);

  private TrustMarkValidator() {
  }

  /**
   * Checks the claims and header of a trust mark (Section 7.3, steps 2 and 4 to 6).
   *
   * @param trustMark the trust mark
   * @param subject the entity whose Entity Configuration contains the trust mark
   * @param now the current time
   * @return a description of the first failed check, or null if the checks pass
   * @throws ParseException if the trust mark claims cannot be parsed
   */
  public static String checkClaims(final SignedJWT trustMark, final String subject, final Instant now)
      throws ParseException {
    final JWTClaimsSet claims = trustMark.getJWTClaimsSet();
    if (EntityStatementClaims.getTrustMarkType(trustMark) == null) {
      return "trust_mark_type is missing";
    }
    if (!subject.equals(claims.getSubject())) {
      return "sub does not match the entity";
    }
    if (trustMark.getHeader().getType() == null
        || !TRUST_MARK_JWT_TYPE.equals(trustMark.getHeader().getType().getType())) {
      return "wrong typ";
    }
    if (claims.getIssuer() == null) {
      return "iss is missing";
    }
    if (claims.getIssueTime() == null) {
      return "iat is missing";
    }
    if (claims.getIssueTime().toInstant().isAfter(now.plus(CLOCK_SKEW))) {
      return "iat is in the future";
    }
    if (claims.getExpirationTime() != null && claims.getExpirationTime().toInstant().isBefore(now)) {
      return "trust mark has expired";
    }
    return null;
  }

  /**
   * Checks the {@code delegation} claim of a trust mark whose type has an owner (Section 7.2.2).
   *
   * @param trustMark the trust mark
   * @param trustMarkType the trust mark type
   * @param ownerId the entity identifier of the trust mark owner
   * @param ownerKeys the keys of the trust mark owner
   * @param now the current time
   * @return a description of the first failed check, or null if the delegation is valid
   * @throws ParseException if the trust mark or the delegation cannot be parsed
   */
  public static String checkDelegation(final SignedJWT trustMark, final String trustMarkType, final String ownerId,
      final JWKSet ownerKeys, final Instant now) throws ParseException {
    final JWTClaimsSet trustMarkClaims = trustMark.getJWTClaimsSet();
    final String delegationJwt = trustMarkClaims.getStringClaim("delegation");
    if (delegationJwt == null) {
      return "delegation is missing";
    }
    final SignedJWT delegation = SignedJWT.parse(delegationJwt);
    final JWTClaimsSet claims = delegation.getJWTClaimsSet();
    if (delegation.getHeader().getType() == null
        || !TrustMarkDelegation.DELEGATION_TYPE.equals(delegation.getHeader().getType().getType())) {
      return "wrong typ in delegation";
    }
    if (!ownerId.equals(claims.getIssuer())) {
      return "iss is not the trust mark owner in delegation";
    }
    if (trustMarkClaims.getIssuer() == null || !trustMarkClaims.getIssuer().equals(claims.getSubject())) {
      return "sub is not the trust mark issuer in delegation";
    }
    if (!trustMarkType.equals(claims.getStringClaim("trust_mark_type"))) {
      return "trust_mark_type does not match in delegation";
    }
    if (claims.getIssueTime() == null) {
      return "iat is missing in delegation";
    }
    if (claims.getExpirationTime() != null && claims.getExpirationTime().toInstant().isBefore(now)) {
      return "delegation has expired";
    }
    if (!verify(delegation, ownerKeys)) {
      return "delegation signature is not valid for the owner keys";
    }
    return null;
  }

  /**
   * Verifies the signature of a JWT against the keys in a JWK set. When the JWT has a {@code kid}, only the key with
   * that identifier is used.
   *
   * @param jwt the JWT to verify
   * @param jwks the keys to verify against
   * @return true if the signature is valid
   */
  public static boolean verify(final SignedJWT jwt, final JWKSet jwks) {
    final String kid = jwt.getHeader().getKeyID();
    final List<JWK> keys = kid == null
        ? jwks.getKeys()
        : new JWKSelector(new JWKMatcher.Builder().keyID(kid).build()).select(jwks);
    for (final JWK jwk : keys) {
      try {
        final Key key = switch (jwk.getKeyType().getValue()) {
          case "EC" -> jwk.toECKey().toPublicKey();
          case "RSA" -> jwk.toRSAKey().toPublicKey();
          case null, default -> null;
        };
        if (key != null && jwt.verify(new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), key))) {
          return true;
        }
      }
      catch (final JOSEException e) {
        log.debug("Failed to verify signature with key '{}': {}", jwk.getKeyID(), e.getMessage());
      }
    }
    return false;
  }
}
