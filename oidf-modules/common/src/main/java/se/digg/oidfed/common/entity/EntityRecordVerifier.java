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
package se.digg.oidfed.common.entity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.Key;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verifier for {@link EntityRecord}
 *
 * @author Felix Hellman
 */
public class EntityRecordVerifier {

  private final JWKSet jwks;

  /**
   * Constructor.
   *
   * @param jwks to trust
   */
  public EntityRecordVerifier(final JWKSet jwks) {
    this.jwks = jwks;
  }

  /**
   * @param jwtString containing entity records
   * @return list of entity records
   */
  public List<EntityRecord> verifyEntities(final String jwtString) {
    try {


      final List<Object> records = this.verify(jwtString)
          .getJWTClaimsSet()
          .getListClaim("entity_records");

      return records.stream().map(record -> {
        try {
          return EntityRecord.fromJson((Map<String, Object>) record);
        } catch (final ParseException e) {
          throw new RuntimeException(e);
        }
      }).toList();
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify entity record", e);
    }
  }

  /**
   * @param jwtString to verify
   * @return policy record
   */
  public Optional<PolicyRecord> verifyPolicy(final String jwtString) {
    try {
          final Object policy = this.verify(jwtString)
          .getJWTClaimsSet()
          .getClaim("policy_record");

      return Optional.ofNullable(policy)
          .map(Map.class::cast)
          .map(PolicyRecord::fromJson);
    } catch (final ParseException | JOSEException e) {
      throw new RecordVerificationException("Failed to verify policy record", e);
    }
  }

  protected SignedJWT verify(final String jwtString) throws JOSEException, ParseException {
    final SignedJWT jwt = SignedJWT.parse(jwtString);
    final Key key = this.selectKey(jwt);

    final JWSVerifier jwsVerifier = new DefaultJWSVerifierFactory()
        .createJWSVerifier(jwt.getHeader(), key);

    if(!jwt.verify(jwsVerifier)){
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
