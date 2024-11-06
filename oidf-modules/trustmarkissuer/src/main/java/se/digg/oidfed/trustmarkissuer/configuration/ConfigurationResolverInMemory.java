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
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertNotEmpty;
import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertTrue;

/**
 * openid-federation-services
 *
 * @author Per Fredrik Plars
 */
public class ConfigurationResolverInMemory implements TrustMarkConfigurationResolver {

  private final List<TrustMarkIssuerProperties> store = new ArrayList<>();
  private final TrustMarkProperties trustMarkProperties;
  private final String issuer;
  private final JWKSet jwkSet;

  public ConfigurationResolverInMemory(final JWKSet jwkSet, final TrustMarkProperties trustMarkProperties,
      final String issuer) {
    this.jwkSet = assertNotEmpty(jwkSet, "Expected jwkSet");
    assertTrue(!jwkSet.isEmpty(), "Expected keys in JWKSet, it is empty");
    this.trustMarkProperties = assertNotEmpty(trustMarkProperties, "Expected trustMarkProperties");
    trustMarkProperties.validate();
    this.issuer = assertNotEmpty(issuer, "Expected issuer");
  }

  public void addTrustMarkIssuerProperties(TrustMarkIssuerProperties trustMarkIssuerProperties) {
    trustMarkIssuerProperties.validate();
    trustMarkIssuerProperties.getSubjects()
        .forEach(TrustMarkIssuerProperties.TrustMarkIssuerSubjectProperties::validate);
    store.add(trustMarkIssuerProperties);
  }

  public void clearTrustMarkIssuerProperties() {
    store.clear();
  }

  @Override
  public Optional<TrustMarkIssuerProperties> getTrustMarkFromTrustMarkId(final TrustMarkId trustMarkId) {
    return store.stream()
        .filter(trustMarkIssuerProperties -> trustMarkIssuerProperties.getTrustmarkid().equals(trustMarkId))
        .findFirst();
  }

  @Override
  public String getIssuer() {
    return issuer;
  }

  @Override
  public TrustMarkProperties getTrustMarkProperties() {
    return trustMarkProperties;
  }

  @Override
  public Optional<SignedJWT> getDelegation(final TrustMarkId trustMarkId) {
    return Optional.empty();
  }

  @Override
  public Optional<JWK> findKey(final JWKSelector keySelector) {
    return keySelector.select(jwkSet).stream().findFirst();
  }

  @Override
  public JWK getSignKey() {
    return jwkSet.getKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("No SignKeyFound"));
  }
}
