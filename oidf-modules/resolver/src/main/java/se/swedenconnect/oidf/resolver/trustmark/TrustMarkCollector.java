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
package se.swedenconnect.oidf.resolver.trustmark;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Responsible for collecting trust marks from a trust chain.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkCollector {

  /**
   * Constructor.
   *
   */
  public TrustMarkCollector() {

  }


  /**
   * Collects and filters trust marks from the given trust chain.
   *
   * @param chain the resolved trust chain including the leaf entity
   * @return list of valid trust mark entries
   */
  public static List<TrustMarkEntry> collectSubjectTrustMarks(final ResolverTrustChain chain)
      throws java.text.ParseException {
    final List<SignedJWT> trustChain = chain.getTrustChain().stream().toList();
    final SignedJWT leafStatement = trustChain.getFirst();
    final SignedJWT trustAnchor = trustChain.getLast();
    if (EntityStatementClaims.getTrustMarks(leafStatement) == null) {
      return List.of();
    }
    final SignedJWT superiorStatement = trustChain.get(2);
    final String subject = EntityStatementClaims.claims(leafStatement).getSubject();

    final List<TrustMarkEntry> trustMarks = TrustMarkCollector.parseTrustMark(leafStatement);
    if (EntityStatementClaims.claims(superiorStatement).getSubject().equals(subject)) {
      // If the superior statement is issued for the subject,
      // then collect any trust marks not present in the leaf statement
      final List<TrustMarkEntry> superiorStatementTrustMarks = TrustMarkCollector.parseTrustMark(superiorStatement);
      superiorStatementTrustMarks.stream()
          .filter(supTrustMark -> trustMarks.stream()
              .noneMatch(subjTrustMark -> supTrustMark.getID().equals(subjTrustMark.getID())))
          .forEach(trustMarks::add);
    }

    final Map<String, Object> trustMarkOwners =
        Optional.ofNullable(EntityStatementClaims.claims(trustAnchor).getJSONObjectClaim("trust_mark_owners"))
            .orElseGet(Map::of);
    trustMarks.removeIf(tm -> !TrustMarkCollector.isDelegationValid(tm, trustMarkOwners));

    final Map<String, Object> trustMarkIssuer = EntityStatementClaims.claims(trustAnchor)
        .getJSONObjectClaim("trust_mark_issuers");
    List<TrustMarkEntry> filtered = trustMarks;
    if (Objects.nonNull(trustMarkIssuer)) {
      final Map<Identifier, List<Issuer>> trustMarkToIssuersMap = getTrustMarkToIssuersMap(trustMarkIssuer);

      if (!trustMarkToIssuersMap.isEmpty()) {
        filtered = trustMarks.stream()
            .filter(tm -> TrustMarkCollector.isTrustMarkAllowed(tm, trustMarkToIssuersMap))
            .toList();
      }
    }

    return filtered.stream().filter(jwt -> {
      final String trustMarkType = EntityStatementClaims.getTrustMarkType(jwt.getTrustMark());
      if (trustMarkType == null) {
        return false;
      }
      final Optional<TrustMarkStatusResponse> trustMarkStatus =
          Optional.ofNullable(chain.getLeafEntity().getTrustMarkStatuses().get(trustMarkType));
      return trustMarkStatus.map(tms -> {
            if (tms.isError()) {
              return true;
            }
            try {
              final Map<String, Object> tmsClaims = tms.getSignedJWT().getJWTClaimsSet().getClaims();
              return tmsClaims.containsKey("status") && "active".equals(tmsClaims.get("status"));
            } catch (final java.text.ParseException e) {
              return false;
            }
          })
          .orElse(false);
    }).toList();
  }

  /**
   * Parses the {@code trust_mark_issuers} claim, a JSON object mapping each trust mark type to an array of
   * entity identifiers (OpenID Federation 1.0, Section 3.1.2). Values that are not arrays, and array elements that
   * are not strings, are ignored.
   *
   * @param trustMarkIssuer the claim value
   * @return map of trust mark type to allowed issuers
   */
  private static Map<Identifier, List<Issuer>> getTrustMarkToIssuersMap(final Map<String, Object> trustMarkIssuer) {
    return trustMarkIssuer
        .entrySet()
        .stream()
        .collect(Collectors.toMap(kv -> new Identifier(kv.getKey()),
            kv -> kv.getValue() instanceof final List<?> values
                ? values.stream()
                    .filter(String.class::isInstance)
                    .map(value -> new Issuer((String) value))
                    .toList()
                : List.of()));
  }

  private static boolean isTrustMarkAllowed(final TrustMarkEntry entry,
                                            final Map<Identifier, List<Issuer>> trustMarkToIssuersMap) {
    final List<Issuer> issuers = trustMarkToIssuersMap.get(entry.getID());
    if (Objects.isNull(issuers) || issuers.isEmpty()) {
      return false;
    }
    final String issuer;
    try {
      issuer = entry.getTrustMark().getJWTClaimsSet().getIssuer();
    } catch (final java.text.ParseException e) {
      log.warn("Failed to parse trust mark, skipping ...", e);
      return false;
    }
    return issuers.contains(new Issuer(issuer));
  }

  private static List<TrustMarkEntry> parseTrustMark(final SignedJWT entity) {
    return new ArrayList<>(Optional.ofNullable(EntityStatementClaims.getTrustMarks(entity)).orElseGet(List::of));
  }

  /**
   * Checks the delegation of a trust mark whose type is listed in {@code trust_mark_owners} of the Trust Anchor
   * (OpenID Federation 1.0, Sections 7.2.2 and 7.3). Trust marks of types without an owner need no delegation.
   *
   * @param entry the trust mark entry
   * @param trustMarkOwners the {@code trust_mark_owners} claim of the Trust Anchor
   * @return true if the trust mark has no owner, or has a valid delegation from its owner
   */
  @SuppressWarnings("unchecked")
  private static boolean isDelegationValid(final TrustMarkEntry entry, final Map<String, Object> trustMarkOwners) {
    final SignedJWT trustMark = entry.getTrustMark();
    final String trustMarkType = EntityStatementClaims.getTrustMarkType(trustMark);
    if (trustMarkType == null || !trustMarkOwners.containsKey(trustMarkType)) {
      return true;
    }
    try {
      if (!(trustMarkOwners.get(trustMarkType) instanceof final Map<?, ?> owner)
          || !(owner.get("sub") instanceof final String ownerId)
          || !(owner.get("jwks") instanceof final Map<?, ?> ownerJwks)) {
        log.info("Trust mark of type '{}' rejected: invalid trust_mark_owners entry in Trust Anchor", trustMarkType);
        return false;
      }
      final JWTClaimsSet trustMarkClaims = trustMark.getJWTClaimsSet();
      final String delegationJwt = trustMarkClaims.getStringClaim("delegation");
      if (delegationJwt == null) {
        log.info("Trust mark of type '{}' from '{}' rejected: delegation is missing",
            trustMarkType, trustMarkClaims.getIssuer());
        return false;
      }
      final SignedJWT delegation = SignedJWT.parse(delegationJwt);
      final JWTClaimsSet delegationClaims = delegation.getJWTClaimsSet();
      final String error;
      if (delegation.getHeader().getType() == null
          || !TrustMarkDelegation.DELEGATION_TYPE.equals(delegation.getHeader().getType().getType())) {
        error = "wrong typ";
      }
      else if (!ownerId.equals(delegationClaims.getIssuer())) {
        error = "iss is not the trust mark owner";
      }
      else if (!Objects.equals(trustMarkClaims.getIssuer(), delegationClaims.getSubject())) {
        error = "sub is not the trust mark issuer";
      }
      else if (!trustMarkType.equals(delegationClaims.getStringClaim("trust_mark_type"))) {
        error = "trust_mark_type does not match";
      }
      else if (delegationClaims.getIssueTime() == null) {
        error = "iat is missing";
      }
      else if (delegationClaims.getExpirationTime() != null
          && delegationClaims.getExpirationTime().toInstant().isBefore(Instant.now())) {
        error = "delegation has expired";
      }
      else if (!TrustMarkCollector.verify(delegation, JWKSet.parse((Map<String, Object>) ownerJwks))) {
        error = "signature is not valid for the owner keys";
      }
      else {
        return true;
      }
      log.info("Trust mark of type '{}' from '{}' rejected: {} in delegation",
          trustMarkType, trustMarkClaims.getIssuer(), error);
      return false;
    }
    catch (final java.text.ParseException e) {
      log.info("Trust mark of type '{}' rejected: failed to parse delegation: {}", trustMarkType, e.getMessage());
      return false;
    }
  }

  /**
   * Verifies the signature of a JWT against the keys in a JWK set. When the JWT has a {@code kid}, only the key with
   * that identifier is used.
   *
   * @param jwt the JWT to verify
   * @param jwks the keys to verify against
   * @return true if the signature is valid
   */
  private static boolean verify(final SignedJWT jwt, final JWKSet jwks) {
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
        if (key != null
            && jwt.verify(new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), key))) {
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
