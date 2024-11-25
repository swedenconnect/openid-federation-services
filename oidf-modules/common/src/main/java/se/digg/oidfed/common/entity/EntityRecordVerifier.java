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
   * @param jwt containing entity records
   * @return list of entity records
   * @throws JOSEException  if signature fails
   * @throws ParseException if entity claims is missing
   */
  public List<EntityRecord> verify(final SignedJWT jwt) throws JOSEException, ParseException {
    final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
        .keyID(jwt.getHeader().getKeyID())
        .build());

    final JWK jwk = selector.select(jwks).getFirst();

    final Key key = switch (jwk.getKeyType().getValue()) {
      case "EC" -> jwk.toECKey().toKeyPair().getPublic();
      case "RSA" -> jwk.toRSAKey().toKeyPair().getPublic();
      case null, default -> throw new IllegalArgumentException("Unspported key type");
    };

    final JWSVerifier jwsVerifier = new DefaultJWSVerifierFactory()
        .createJWSVerifier(jwt.getHeader(), key);

    jwt.verify(jwsVerifier);

    final List<Object> records = jwt
        .getJWTClaimsSet()
        .getListClaim("entity_records");

    return records.stream().map(record -> {
      try {
        return EntityRecord.fromJson((Map<String, Object>) record);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }).toList();
  }
}
