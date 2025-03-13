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
package se.digg.oidfed.common.jwt;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;

/**
 * Factory class for creating signer.
 *
 * @author Felix Hellman
 */
public class JWKSetSignerFactory implements SignerFactory {
  private final JWKSet jwkSet;

  /**
   * @return new signer
   */
  public FederationSigner createSigner() {
    return new JWKFederationSigner(this.getSignKey());
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
   *
   * @param jwkSet to use
   */
  public JWKSetSignerFactory(final JWKSet jwkSet) {
    this.jwkSet = jwkSet;
  }


}
