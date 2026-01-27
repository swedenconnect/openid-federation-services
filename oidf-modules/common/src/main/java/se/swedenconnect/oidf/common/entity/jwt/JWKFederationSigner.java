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
package se.swedenconnect.oidf.common.entity.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import lombok.Getter;

/**
 * JWK Implementation Signer used for various federation components.
 *
 * @author Felix Hellman
 */
@Getter
public class JWKFederationSigner implements FederationSigner {

  private final JWK signKey;

  /**
   * @param signKey for this signer
   */
  public JWKFederationSigner(final JWK signKey) {
    this.signKey = signKey;
  }

  /**
   * Signs claims
   *
   * @param type   for this jwt
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
        .keyID(this.signKey.getKeyID())
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

  private JWSVerifier getVerifier(final JWK signingKey) throws JOSEException {
    final KeyType keyType = signingKey.getKeyType();
    if (keyType.equals(KeyType.EC)) {
      return new ECDSAVerifier(signingKey.toECKey());
    }
    if (keyType.equals(KeyType.RSA)) {
      return new RSASSAVerifier(signingKey.toRSAKey());
    }
    throw new JOSEException("Unsupported key type");
  }

  @Override
  public boolean verify(final String jwt) {
    try {
      return SignedJWT.parse(jwt).verify(this.getVerifier(this.signKey));
    } catch (final JOSEException | java.text.ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
