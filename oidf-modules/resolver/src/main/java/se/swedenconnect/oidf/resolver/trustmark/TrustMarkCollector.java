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
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.resolver.tree.ResolverTrustChain;

import java.security.Key;
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
    final Map<String, Object> trustMarkOwners =
        Optional.ofNullable(EntityStatementClaims.claims(trustAnchor).getJSONObjectClaim("trust_mark_owners"))
            .orElseGet(Map::of);
    trustMarkOwners.keySet().forEach(key -> {
      trustMarks.stream().filter(k -> k.getID().getValue().equals(key))
          .forEach(tm -> {
            final Map<String, Object> trustMarkOwner = (Map<String, Object>) trustMarkOwners.get(key);
            try {
              final JWKSet parsed = JWKSet.parse((Map<String, Object>) trustMarkOwner.get("jwks"));
              tm.getTrustMark()
                  .verify(new DefaultJWSVerifierFactory()
                      .createJWSVerifier(
                          tm.getTrustMark().getHeader(),
                          selectKey(tm.getTrustMark(), parsed)
                      )
                  );
            } catch (java.text.ParseException | JOSEException e) {
              throw new RuntimeException(e);
            }
          });

    });
    if (EntityStatementClaims.claims(superiorStatement).getSubject().equals(subject)) {
      // If the superior statement is issued for the subject,
      // then collect any trust marks not present in the leaf statement
      final List<TrustMarkEntry> superiorStatementTrustMarks = TrustMarkCollector.parseTrustMark(superiorStatement);
      superiorStatementTrustMarks.stream()
          .filter(supTrustMark -> trustMarks.stream()
              .noneMatch(subjTrustMark -> supTrustMark.getID().equals(subjTrustMark.getID())))
          .forEach(trustMarks::add);
    }


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
      try {
        final JWTClaimsSet claims = jwt.getTrustMark().getJWTClaimsSet();
        final String trustMarkType = claims.getClaimAsString("trust_mark_type");
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
      } catch (final java.text.ParseException e) {
        throw new RuntimeException(e);
      }
    }).toList();
  }

  @SuppressWarnings("unchecked")
  private static Map<Identifier, List<Issuer>> getTrustMarkToIssuersMap(final Map<String, Object> trustMarkIssuer) {
    return trustMarkIssuer
        .entrySet()
        .stream()
        .collect(Collectors.toMap(kv -> new Identifier(kv.getKey()),
            kv -> ((List<Object>) kv.getValue())
                .stream()
                .map(value -> (Map<String, Object>) value)
                .map(value -> new Issuer((String) value.get("value")))
                .toList()));
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

  @SuppressWarnings("unchecked")
  private static List<TrustMarkEntry> parseTrustMark(final SignedJWT entity) throws java.text.ParseException {
    final List<Object> trustMarks =
        Optional.ofNullable(EntityStatementClaims.claims(entity).getListClaim("trust_marks")).orElseGet(List::of);
    return trustMarks.stream()
        .map(o -> new JSONObject((Map<String, Object>) o))
        .map(json -> {
          try {
            return TrustMarkEntry.parse(json);
          } catch (final ParseException e) {
            throw new IllegalArgumentException("Failed to parse TrustMarkEntry", e);
          }
        }).toList();
  }

  protected static Key selectKey(final SignedJWT jwt, final JWKSet jwks) throws JOSEException {
    final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
        .keyID(jwt.getHeader().getKeyID())
        .build());

    final JWK jwk = selector
        .select(jwks)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unable to resolve key for JWT with kid:'%s' "
            .formatted(jwt.getHeader().getKeyID())));

    return switch (jwk.getKeyType().getValue()) {
      case "EC" -> jwk.toECKey().toKeyPair().getPublic();
      case "RSA" -> jwk.toRSAKey().toKeyPair().getPublic();
      case null, default -> throw new IllegalArgumentException("Unsupported key type");
    };
  }
}
