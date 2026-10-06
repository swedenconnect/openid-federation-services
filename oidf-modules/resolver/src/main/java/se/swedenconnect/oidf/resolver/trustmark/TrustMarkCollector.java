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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkValidator;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Responsible for collecting trust marks from a trust chain.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkCollector {

  private static final String STATUS_RESPONSE_TYPE = "trust-mark-status-response+jwt";


  /**
   * Constructor.
   *
   */
  public TrustMarkCollector() {

  }


  /**
   * Collects and filters trust marks from the given trust chain. A trust mark is kept only if it passes the checks
   * in OpenID Federation 1.0, Section 7.3, and its issuer reports it as active, or publishes no status endpoint.
   *
   * @param chain the resolved trust chain including the leaf entity
   * @param issuerKeyResolver resolves the federation entity keys of a trust mark issuer, empty if the issuer cannot
   *     be trusted
   * @return list of valid trust mark entries
   */
  public static List<TrustMarkEntry> collectSubjectTrustMarks(final ResolverTrustChain chain,
      final Function<String, Optional<JWKSet>> issuerKeyResolver)
      throws java.text.ParseException {
    final List<SignedJWT> trustChain = chain.getTrustChain().stream().toList();
    final SignedJWT leafStatement = trustChain.getFirst();
    final SignedJWT trustAnchor = trustChain.getLast();
    if (EntityStatementClaims.getTrustMarks(leafStatement) == null) {
      return List.of();
    }
    final String subject = EntityStatementClaims.claims(leafStatement).getSubject();

    // Trust marks are only allowed in Entity Configurations (Section 3.2, step 20)
    final List<TrustMarkEntry> trustMarks = TrustMarkCollector.parseTrustMark(leafStatement);

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

    final Map<String, Optional<JWKSet>> issuerKeys = new HashMap<>();
    final Map<String, TrustMarkStatusResponse> statuses = chain.getLeafEntity().getTrustMarkStatuses();
    return filtered.stream()
        .filter(tm -> TrustMarkCollector.isTrustMarkValid(tm, subject,
            issuer -> issuerKeys.computeIfAbsent(issuer, issuerKeyResolver), statuses))
        .toList();
  }

  /**
   * Validates a trust mark according to OpenID Federation 1.0, Section 7.3, and checks its status with the issuer
   * when the issuer publishes a status endpoint.
   *
   * @param entry the trust mark entry
   * @param subject the entity the trust mark is presented for
   * @param issuerKeys resolves the federation entity keys of the issuer
   * @param statuses status responses from the issuers, keyed by the serialized trust mark JWT
   * @return true if the trust mark is valid and active
   */
  private static boolean isTrustMarkValid(final TrustMarkEntry entry, final String subject,
      final Function<String, Optional<JWKSet>> issuerKeys, final Map<String, TrustMarkStatusResponse> statuses) {
    final SignedJWT trustMark = entry.getTrustMark();
    final String trustMarkType = EntityStatementClaims.getTrustMarkType(trustMark);
    try {
      final JWTClaimsSet claims = trustMark.getJWTClaimsSet();
      final String error = TrustMarkCollector.validateTrustMark(trustMark, claims, entry.getID().getValue(),
          trustMarkType, subject, issuerKeys, statuses);
      if (error == null) {
        return true;
      }
      log.info("Trust mark of type '{}' from '{}' for '{}' rejected: {}",
          trustMarkType, claims.getIssuer(), subject, error);
      return false;
    }
    catch (final java.text.ParseException e) {
      log.info("Trust mark of type '{}' for '{}' rejected: failed to parse: {}", trustMarkType, subject,
          e.getMessage());
      return false;
    }
  }

  /**
   * Performs the trust mark checks.
   *
   * @param trustMark the trust mark JWT
   * @param claims the trust mark claims
   * @param entryType the trust mark type given by the {@code trust_marks} entry
   * @param trustMarkType the trust mark type of the trust mark itself
   * @param subject the entity the trust mark is presented for
   * @param issuerKeys resolves the federation entity keys of the issuer
   * @param statuses status responses from the issuers, keyed by the serialized trust mark JWT
   * @return a description of the first failed check, or null if the trust mark is valid
   * @throws java.text.ParseException if the status response cannot be parsed
   */
  private static String validateTrustMark(final SignedJWT trustMark, final JWTClaimsSet claims,
      final String entryType, final String trustMarkType, final String subject,
      final Function<String, Optional<JWKSet>> issuerKeys,
      final Map<String, TrustMarkStatusResponse> statuses) throws java.text.ParseException {
    final String claimsError = TrustMarkValidator.checkClaims(trustMark, subject, Instant.now());
    if (claimsError != null) {
      return claimsError;
    }
    if (!trustMarkType.equals(entryType)) {
      return "trust_mark_type of the entry and the trust mark differ";
    }
    final Optional<JWKSet> keys = issuerKeys.apply(claims.getIssuer());
    if (keys.isEmpty()) {
      return "issuer keys could not be resolved through a trust chain";
    }
    if (!TrustMarkValidator.verify(trustMark, keys.get())) {
      return "signature is not valid for the issuer keys";
    }
    final TrustMarkStatusResponse status = statuses.get(trustMark.serialize());
    if (status != null && status.isNoStatusEndpoint()) {
      // The issuer publishes no status endpoint, the local checks above are all that can be done (Section 7.3)
      return null;
    }
    if (status == null || status.isError() || status.getSignedJWT() == null) {
      return "status could not be obtained from the issuer";
    }
    return TrustMarkCollector.validateStatus(status.getSignedJWT(), trustMark, claims.getIssuer(), keys.get());
  }

  /**
   * Validates a trust mark status response according to OpenID Federation 1.0, Section 8.4.2.
   *
   * @param response the status response
   * @param trustMark the trust mark the status was requested for
   * @param issuer the trust mark issuer
   * @param issuerKeys the federation entity keys of the issuer
   * @return a description of the first failed check, or null if the response says the trust mark is active
   * @throws java.text.ParseException if the response cannot be parsed
   */
  private static String validateStatus(final SignedJWT response, final SignedJWT trustMark, final String issuer,
      final JWKSet issuerKeys) throws java.text.ParseException {
    if (response.getHeader().getType() == null
        || !STATUS_RESPONSE_TYPE.equals(response.getHeader().getType().getType())) {
      return "status response has wrong typ";
    }
    if (!TrustMarkValidator.verify(response, issuerKeys)) {
      return "status response signature is not valid for the issuer keys";
    }
    final JWTClaimsSet claims = response.getJWTClaimsSet();
    if (!issuer.equals(claims.getIssuer())) {
      return "status response iss is not the trust mark issuer";
    }
    if (!trustMark.serialize().equals(claims.getStringClaim("trust_mark"))) {
      return "status response is for another trust mark";
    }
    final String status = claims.getStringClaim("status");
    return "active".equals(status) ? null : "status is '%s'".formatted(status);
  }

  /**
   * Parses the {@code trust_mark_issuers} claim, a JSON object mapping each trust mark type to an array of
   * entity identifiers (OpenID Federation 1.0, Section 3.1.2). A type whose value is not an array of strings is
   * mapped to null, so that no trust marks of that type are accepted.
   *
   * @param trustMarkIssuer the claim value
   * @return map of trust mark type to allowed issuers, where an empty list means that anyone may issue
   */
  private static Map<Identifier, List<Issuer>> getTrustMarkToIssuersMap(final Map<String, Object> trustMarkIssuer) {
    final Map<Identifier, List<Issuer>> issuers = new HashMap<>();
    trustMarkIssuer.forEach((type, value) -> {
      if (value instanceof final List<?> values && values.stream().allMatch(String.class::isInstance)) {
        issuers.put(new Identifier(type), values.stream().map(v -> new Issuer((String) v)).toList());
      }
      else {
        log.info("Invalid trust_mark_issuers value for trust mark type '{}', no issuer is trusted", type);
        issuers.put(new Identifier(type), null);
      }
    });
    return issuers;
  }

  /**
   * Checks whether the Trust Anchor trusts the issuer of a trust mark.
   *
   * @param entry the trust mark entry
   * @param trustMarkToIssuersMap the parsed {@code trust_mark_issuers} claim
   * @return true if the trust mark type is listed and its issuer is trusted, or the type allows any issuer
   */
  private static boolean isTrustMarkAllowed(final TrustMarkEntry entry,
                                            final Map<Identifier, List<Issuer>> trustMarkToIssuersMap) {
    final List<Issuer> issuers = trustMarkToIssuersMap.get(entry.getID());
    if (Objects.isNull(issuers)) {
      return false;
    }
    if (issuers.isEmpty()) {
      // An empty array means that anyone may issue trust marks of this type
      return true;
    }
    final String issuer;
    try {
      issuer = entry.getTrustMark().getJWTClaimsSet().getIssuer();
    } catch (final java.text.ParseException e) {
      log.info("Trust mark of type '{}' rejected: failed to parse: {}", entry.getID(), e.getMessage());
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
      final String error = TrustMarkValidator.checkDelegation(trustMark, trustMarkType, ownerId,
          JWKSet.parse((Map<String, Object>) ownerJwks), Instant.now());
      if (error == null) {
        return true;
      }
      log.info("Trust mark of type '{}' from '{}' rejected: {}",
          trustMarkType, trustMark.getJWTClaimsSet().getIssuer(), error);
      return false;
    }
    catch (final java.text.ParseException e) {
      log.info("Trust mark of type '{}' rejected: failed to parse delegation: {}", trustMarkType, e.getMessage());
      return false;
    }
  }
}
