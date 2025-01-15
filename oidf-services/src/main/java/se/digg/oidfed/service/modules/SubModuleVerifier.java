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
package se.digg.oidfed.service.modules;

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
import java.util.Map;

/**
 * Verifier for sub module payload.
 *
 * @author Felix Hellman
 */
public class SubModuleVerifier {
  private final JWKSet jwks;

  /**
   * Constructor.
   * @param jwks for verifying signatures.
   */
  public SubModuleVerifier(final JWKSet jwks) {
    this.jwks = jwks;
  }

  /**
   * Verifies jwt from registry.
   * @param jwt to verify
   * @return response
   * @throws ParseException
   * @throws JOSEException
   */
  public ModuleResponse verify(final String jwt) throws ParseException, JOSEException {
    final SignedJWT signedJWT = SignedJWT.parse(jwt);
    final Key key = this.selectKey(signedJWT);
    final JWSVerifier verifier = new DefaultJWSVerifierFactory()
        .createJWSVerifier(signedJWT.getHeader(), key);
    if (signedJWT.verify(verifier)) {
      final Map<String, Object> json = signedJWT.getJWTClaimsSet().getJSONObjectClaim("modules");
      return ModuleResponse.fromJson(json);
    }
    throw new IllegalArgumentException("Failed to validate signature");
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
