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
package se.digg.oidfed.common.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;

/**
 * Factory class for creating signer.
 *
 * @author Felix Hellman
 */
public class SignerFactory {
  private final JWKSet jwkSet;

  /**
   * @return new signer
   */
  public FederationSigner createSigner() {
    return new FederationSigner(this.getSignKey());
  }

  /**
   * @return current sign key
   */
  public JWK getSignKey() {
    return new JWKSelector(new JWKMatcher.Builder().build()).select(this.jwkSet).getFirst();
  }

  /**
   * @return all current keys
   */
  public JWKSet getSignKeys() {
    return this.jwkSet;
  }

  /**
   * Constructor.
   * @param jwkSet to use
   */
  public SignerFactory(final JWKSet jwkSet) {
    this.jwkSet = jwkSet;
  }

  /**
   * Signer used for various federation components.
   * @param signKey for this signer
   *
   * @author Felix Hellman
   */
  public record FederationSigner(JWK signKey) {
    /**
     * Signs claims
     * @param type for this jwt
     * @param claims fot this jwt
     * @return a signed jwt
     * @throws JOSEException
     * @throws ParseException
     */
    public SignedJWT sign(final JOSEObjectType type, final JWTClaimsSet claims) throws JOSEException, ParseException {
      final JWSSigner signer = this.getSigner(this.signKey);
      final JWSAlgorithm alg = signer.supportedJWSAlgorithms().stream().findFirst().get();
      final JWSHeader header = new JWSHeader.Builder(alg)
          .type(type)
          .build();
      final SignedJWT signedJWT = new SignedJWT(header, claims);
      signedJWT.sign(signer);
      return signedJWT;
    }

    private JWSSigner getSigner(final JWK signingKey) throws JOSEException {
      final KeyType keyType = signingKey.getKeyType();
      if (keyType.equals(KeyType.EC)) {
        return new ECDSASigner(signingKey.toECKey());
      }
      if (keyType.equals(KeyType.RSA)) {
        return new RSASSASigner(signingKey.toRSAKey());
      }
      throw new JOSEException("Unsupported key type");
    }
  }
}
