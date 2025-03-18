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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;

/**
 * Signer used for various federation components.
 *
 * @author Felix Hellman
 */
public interface FederationSigner {
  /**
   * Signs claims
   *
   * @param type   for this jwt
   * @param claims fot this jwt
   * @return a signed jwt
   * @throws JOSEException
   * @throws ParseException
   */
  SignedJWT sign(final JOSEObjectType type, final JWTClaimsSet claims) throws JOSEException, ParseException;
}
