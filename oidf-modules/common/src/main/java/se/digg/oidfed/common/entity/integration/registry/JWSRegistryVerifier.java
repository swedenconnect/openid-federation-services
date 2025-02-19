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
package se.digg.oidfed.common.entity.integration.registry;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.RecordVerificationException;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.validation.FederationAssert;

import java.security.Key;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * JWS implementation of {@link RegistryVerifier}.
 *
 * @author Felix Hellman
 */
public class JWSRegistryVerifier implements RegistryVerifier {

  private final JWKSet jwks;

  /**
   * Constructor.
   *
   * @param jwks to trust
   */
  public JWSRegistryVerifier(final JWKSet jwks) {
    this.jwks = jwks;
  }

  @Override
  public Expirable<List<EntityRecord>> verifyEntityRecords(final String jwt) {
    try {
      final JWTClaimsSet claims = this.verify(jwt)
          .getJWTClaimsSet();

      final List<Object> records = claims
          .getListClaim("entity_records");

      final List<EntityRecord> entities = records.stream().map(record -> {
        try {
          return EntityRecord.fromJson((Map<String, Object>) record);
        } catch (final ParseException e) {
          throw new RuntimeException(e);
        }
      }).toList();
      return new Expirable<>(claims.getExpirationTime().toInstant(), entities);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify entity record", e);
    }
  }

  @Override
  public Expirable<PolicyRecord> verifyPolicyRecord(final String jwt) {
    try {
      final JWTClaimsSet claims = this.verify(jwt)
          .getJWTClaimsSet();
      final Map<String, Object> policy = (Map<String, Object>) claims
          .getClaim("policy_record");

      final PolicyRecord policyRecord = PolicyRecord.fromJson(policy);
      return new Expirable<>(claims.getExpirationTime().toInstant(), policyRecord);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify policy record", e);
    }
  }

  @Override
  public Expirable<ModuleResponse> verifyModuleResponse(final String jwt) {
    try {
      final SignedJWT signedJWT = this.verify(jwt);
      final JWTClaimsSet claims = signedJWT
          .getJWTClaimsSet();
      final Map<String, Object> json = claims
          .getJSONObjectClaim("module_records");
      FederationAssert.assertNotEmpty(json, "Missing claim for:'module_records' ");
      final ModuleResponse moduleResponse = ModuleResponse.fromJson(json);
      FederationAssert.assertNotEmpty(claims.getExpirationTime(), "Missing claim 'exp' in token");

      return new Expirable<>(claims.getExpirationTime().toInstant(), moduleResponse);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify module record", e);
    }
  }

  @Override
  public Expirable<List<TrustMarkSubjectRecord>> verifyTrustMarkSubjects(final String jwt) {
    try {
      final JWTClaimsSet claims = this.verify(jwt)
          .getJWTClaimsSet();
      final List<Object> records = claims
          .getListClaim("trustmark_records");
      FederationAssert.assertNotEmpty(records,"Missing claim for:'trustmark_records' ");
      final List<TrustMarkSubjectRecord> trustMarkSubjectRecords = records.stream()
          .map(o -> (Map<String, Object>) o)
          .map(TrustMarkSubjectRecord::fromJson)
          .toList();
      return new Expirable<>(claims.getExpirationTime().toInstant(), trustMarkSubjectRecords);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify TrustMarkIssuerSubject record", e);
    }
  }

  private SignedJWT verify(final String jwtString) throws JOSEException, ParseException {
    final SignedJWT jwt = SignedJWT.parse(jwtString);
    final Key key = this.selectKey(jwt);

    final JWSVerifier jwsVerifier = new DefaultJWSVerifierFactory()
        .createJWSVerifier(jwt.getHeader(), key);

    if (!jwt.verify(jwsVerifier)) {
      throw new RecordVerificationException("Failed to verify signature on record");
    }
    return jwt;
  }


  protected Key selectKey(final SignedJWT jwt) throws JOSEException {
    final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
        .keyID(jwt.getHeader().getKeyID())
        .build());

    final JWK jwk = selector
        .select(this.jwks)
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
