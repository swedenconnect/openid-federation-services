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
package se.swedenconnect.oidf.resolver.trustmark;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Responsible for collecting trust marks from a trust chain.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkCollector {
  /**
   * @param chain to check
   * @return trust marks from chain
   */
  public static List<TrustMarkEntry> collectSubjectTrustMarks(final List<EntityStatement> chain) {
    final EntityStatement leafStatement = chain.getFirst();
    final EntityStatement trustAnchor = chain.getLast();
    if (leafStatement.getClaimsSet().getTrustMarks() == null) {
      return List.of();
    }
    final EntityStatement superiorStatement = chain.get(2);
    final String subject = leafStatement.getClaimsSet().getSubject().getValue();

    final List<TrustMarkEntry> trustMarks = TrustMarkCollector.parseTrustMark(leafStatement);
    final JSONObject trustMarkOwners = trustAnchor.getClaimsSet().getJSONObjectClaim("trust_mark_owners");
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
    if (superiorStatement.getClaimsSet().getSubject().getValue().equals(subject)) {
      // If the superior statement is issued for the subject,
      // then collect any trust marks not present in the leaf statement
      final List<TrustMarkEntry> superiorStatementTrustMarks = TrustMarkCollector.parseTrustMark(superiorStatement);
      superiorStatementTrustMarks.stream()
          .filter(supTrustMark -> trustMarks.stream()
              .noneMatch(subjTrustMark -> supTrustMark.getID().equals(subjTrustMark.getID())))
          .forEach(trustMarks::add);
    }


    final JSONObject trustMarkIssuer = trustAnchor.getClaimsSet()
        .getJSONObjectClaim("trust_mark_issuers");
    if (Objects.nonNull(trustMarkIssuer)) {
      final Map<Identifier, List<Issuer>> trustMarkToIssuersMap = getTrustMarkToIssuersMap(trustMarkIssuer);

      if (!trustMarkToIssuersMap.isEmpty()) {
        return trustMarks.stream()
            .filter(tm -> TrustMarkCollector.isTrustMarkAllowed(tm, trustMarkToIssuersMap))
            .toList();
      }
    }

    return trustMarks;
  }

  private static Map<Identifier, List<Issuer>> getTrustMarkToIssuersMap(final JSONObject trustMarkIssuer) {
    return trustMarkIssuer
        .entrySet()
        .stream()
        .collect(Collectors.toMap(kv -> new Identifier(kv.getKey()),
            kv -> ((JSONArray) kv.getValue())
                .stream()
                .map(JSONObject.class::cast)
                .map(value -> new Issuer(value.getAsString("value")))
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

  private static List<TrustMarkEntry> parseTrustMark(final EntityStatement entity) {
    final JSONArray trustMarks = entity.getClaimsSet().getJSONArrayClaim("trust_marks");
    return trustMarks.stream().toList().stream()
        .map(JSONObject.class::cast)
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
