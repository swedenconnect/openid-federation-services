/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer.configuration;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jwt.SignedJWT;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.Optional;

/**
 * Resolver for trustmarks
 *
 * @author Per Fredrik Plars
 */
public interface TrustMarkConfigurationResolver {

  /**
   * Defined trust mark issued to subjects
   *
   * @param trustMarkId TrustMarkId for this specific trustmark
   * @return Optional of TrustMarkIssuerProperties, empty if nothing exist. Never null
   */
  Optional<TrustMarkIssuerProperties> getTrustMarkFromTrustMarkId(TrustMarkId trustMarkId);

  /**
   * Issuer for ?!
   *
   * @return String containing Issuer. Null is not accepted.
   */
  String getIssuer();

  /**
   * General properties for how the trust mark issuer will behave. Can be called several times.
   *
   * @return TrustMarkProperties, null is not accepted
   */
  TrustMarkProperties getTrustMarkProperties();

  /**
   * https://openid.net/specs/openid-federation-1_0.html#section-7.2
   *
   * @param trustMarkId TrustMarkid
   * @return SignedJWT Following https://openid.net/specs/openid-federation-1_0.html#section-7.2.1
   */
  Optional<SignedJWT> getDelegation(TrustMarkId trustMarkId);

  /**
   * Uses a JWKSelector to find the key needed to validate signatures
   *
   * @return An optional with JWK to verify signature
   */
  Optional<JWK> findKey(JWKSelector keySelector);

  /**
   * Current key used to sign trustmark statement.
   *
   * @return JWK containing key that will be used to sign a TrustMark. Kid will be used in header.
   */
  JWK getSignKey();
}
