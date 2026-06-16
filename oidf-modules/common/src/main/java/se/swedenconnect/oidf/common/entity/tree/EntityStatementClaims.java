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
package se.swedenconnect.oidf.common.entity.tree;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsVerifier;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import com.nimbusds.openid.connect.sdk.federation.utils.JWTUtils;
import net.minidev.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Typed claims accessors for entity statement {@link SignedJWT}s.
 * <p>
 * Replicates the parts of nimbus' {@code EntityStatementClaimsSet} /
 * {@code CommonFederationClaimsSet} that are useful here, without their
 * hardcoded {@code hasMetadata()}/{@code validateRequiredClaimsPresence()}
 * checks.
 *
 * @author Felix Hellman
 */
public final class EntityStatementClaims {

  /**
   * JOSE object type used for entity statements.
   */
  public static final JOSEObjectType ENTITY_STATEMENT_TYPE = new JOSEObjectType("entity-statement+jwt");

  private EntityStatementClaims() {
  }

  /**
   * @param jwt to get claims from
   * @return claims set of the given jwt
   */
  public static JWTClaimsSet claims(final SignedJWT jwt) {
    try {
      return jwt.getJWTClaimsSet();
    } catch (final java.text.ParseException e) {
      throw new IllegalStateException("Failed to parse entity statement claims", e);
    }
  }

  /**
   * @param jwt to get entity id from
   * @return entity id (subject) of the given jwt
   */
  public static EntityID getEntityID(final SignedJWT jwt) {
    return new EntityID(claims(jwt).getSubject());
  }

  /**
   * @param jwt to check
   * @return true if issuer and subject are equal
   */
  public static boolean isSelfStatement(final SignedJWT jwt) {
    final JWTClaimsSet claimsSet = claims(jwt);
    final String issuer = claimsSet.getIssuer();
    final String subject = claimsSet.getSubject();
    return issuer != null && issuer.equals(subject);
  }

  /**
   * @param jwt to get jwk set from
   * @return jwk set from the {@code jwks} claim, or null if not present or invalid
   */
  public static JWKSet getJWKSet(final SignedJWT jwt) {
    try {
      final Map<String, Object> jwkSet = claims(jwt).getJSONObjectClaim("jwks");
      if (jwkSet == null) {
        return null;
      }
      return JWKSet.parse(jwkSet);
    } catch (final java.text.ParseException e) {
      return null;
    }
  }

  /**
   * @param jwt to get authority hints from
   * @return entity ids from the {@code authority_hints} claim, or null if not present
   */
  public static List<EntityID> getAuthorityHints(final SignedJWT jwt) {
    final List<String> hints;
    try {
      hints = claims(jwt).getStringListClaim("authority_hints");
    } catch (final java.text.ParseException e) {
      return null;
    }
    if (hints == null) {
      return null;
    }
    final List<EntityID> authorityHints = new LinkedList<>();
    for (final String hint : hints) {
      authorityHints.add(new EntityID(hint));
    }
    return authorityHints;
  }

  /**
   * @param jwt  to get metadata from
   * @param type of metadata to get
   * @return metadata for the given type, or null if not present
   */
  public static JSONObject getMetadata(final SignedJWT jwt, final EntityType type) {
    try {
      final Map<String, Object> metadata = claims(jwt).getJSONObjectClaim("metadata");
      if (metadata == null) {
        return null;
      }
      return JSONObjectUtils.getJSONObject(new JSONObject(metadata), type.getValue(), null);
    } catch (final java.text.ParseException | ParseException e) {
      return null;
    }
  }

  /**
   * @param jwt to get federation entity metadata from
   * @return federation entity metadata, or null if not present or invalid
   */
  public static FederationEntityMetadata getFederationEntityMetadata(final SignedJWT jwt) {
    final JSONObject metadata = getMetadata(jwt, EntityType.FEDERATION_ENTITY);
    if (metadata == null) {
      return null;
    }
    try {
      return FederationEntityMetadata.parse(metadata);
    } catch (final ParseException e) {
      return null;
    }
  }

  /**
   * @param jwt  to get metadata policy from
   * @param type of metadata policy to get
   * @return metadata policy for the given type, or null if not present or invalid
   */
  public static MetadataPolicy getMetadataPolicy(final SignedJWT jwt, final EntityType type) {
    try {
      final Map<String, Object> policy = claims(jwt).getJSONObjectClaim("metadata_policy");
      if (policy == null) {
        return null;
      }
      final JSONObject typePolicy = JSONObjectUtils.getJSONObject(new JSONObject(policy), type.getValue(), null);
      if (typePolicy == null) {
        return null;
      }
      return MetadataPolicy.parse(typePolicy);
    } catch (final java.text.ParseException | ParseException | PolicyViolationException e) {
      return null;
    }
  }

  /**
   * @param jwt to get trust marks from
   * @return trust mark entries from the {@code trust_marks} claim, or null if not present or invalid
   */
  @SuppressWarnings("unchecked")
  public static List<TrustMarkEntry> getTrustMarks(final SignedJWT jwt) {
    try {
      final List<Object> array = claims(jwt).getListClaim("trust_marks");
      if (array == null) {
        return null;
      }
      final List<TrustMarkEntry> marks = new LinkedList<>();
      for (final Object o : array) {
        marks.add(TrustMarkEntry.parse(new JSONObject((Map<String, Object>) o)));
      }
      return marks;
    } catch (final java.text.ParseException | ParseException e) {
      return null;
    }
  }

  /**
   * @param jwt to get critical extension claims from
   * @return critical extension claim names from the {@code crit} claim, or null if not present
   */
  public static List<String> getCriticalExtensionClaims(final SignedJWT jwt) {
    try {
      return claims(jwt).getStringListClaim("crit");
    } catch (final java.text.ParseException e) {
      return null;
    }
  }

  /**
   * Signs the given claims set as an entity statement.
   *
   * @param claimsSet  to sign
   * @param signingJWK to sign with
   * @return signed entity statement
   * @throws JOSEException if signing failed
   */
  public static SignedJWT sign(final JWTClaimsSet claimsSet, final JWK signingJWK) throws JOSEException {
    return JWTUtils.sign(signingJWK, JWTUtils.resolveSigningAlgorithm(signingJWK), ENTITY_STATEMENT_TYPE, claimsSet);
  }

  /**
   * Verifies the signature of the given entity statement against the given jwk set.
   *
   * @param jwt    to verify
   * @param jwkSet to verify against
   * @return thumbprint of the jwk used to verify the signature
   * @throws BadJOSEException if the jwt is invalid
   * @throws JOSEException    if the signature verification failed
   */
  public static Base64URL verifySignature(final SignedJWT jwt, final JWKSet jwkSet)
      throws BadJOSEException, JOSEException {
    return JWTUtils.verifySignature(jwt, ENTITY_STATEMENT_TYPE, new EntityStatementClaimsVerifier(null), jwkSet);
  }

  /**
   * Verifies the signature of the given self-signed entity statement against its own jwk set.
   *
   * @param jwt to verify
   * @return thumbprint of the jwk used to verify the signature
   * @throws BadJOSEException if the jwt is invalid
   * @throws JOSEException    if the signature verification failed
   */
  public static Base64URL verifySignatureOfSelfStatement(final SignedJWT jwt)
      throws BadJOSEException, JOSEException {
    return verifySignature(jwt, getJWKSet(jwt));
  }
}
