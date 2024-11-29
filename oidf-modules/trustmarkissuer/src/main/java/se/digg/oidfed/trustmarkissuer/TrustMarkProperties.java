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
 *  limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertNotEmpty;
import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertTrue;

/**
 * Properties for TrustMarkIssuer
 *
 * @param trustMarkValidityDuration The validity duration of issued Trust Marks
 * @param issuerEntityId IssuerEntityId
 * @param signKey Key used to sign this trustmark
 * @param trustMarks TrustMark Issuer
 * @param alias for this trustmark instance
 * @author Per Fredrik Plars
 */
@Builder
@Slf4j
public record TrustMarkProperties(Duration trustMarkValidityDuration, EntityID issuerEntityId,JWK signKey,
    List<TrustMarkIssuerProperties> trustMarks,String alias) {

  /**
   * Validate content of configuration.
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    assertNotEmpty(this.trustMarkValidityDuration, "TrustMarkValidityDuration is expected");
    assertNotEmpty(this.signKey, "SignKey is expected");
    assertNotEmpty(this.issuerEntityId, "IssuerEntityId is expected");
    assertNotEmpty(this.trustMarks, "TrustMarks is expected");
    assertNotEmpty(this.alias, "Alias is expected");
    assertTrue(this.trustMarkValidityDuration.minus(Duration.ofMinutes(4)).isPositive(),
        "Expect trustMarkValidityDuration to be grater than 5 minutes. Current value:'%s'"
            .formatted(this.trustMarkValidityDuration));

    this.trustMarks.forEach(TrustMarkIssuerProperties::validate);

  }

  /**
   *
   * @param trustMarkId The Trust Mark ID
   * @param logoUri Optional logo for issued Trust Marks
   * @param refUri Optional URL to information about issued Trust Marks
   * @param trustMarkIssuerSubjectLoader TrustMarkIssuerSubjectLoader
   * @param delegation TrustMark delegation
   */
  @Builder
  public record TrustMarkIssuerProperties(TrustMarkId trustMarkId, Optional<String> logoUri, Optional<String> refUri,
      Optional<TrustMarkDelegation> delegation,TrustMarkIssuerSubjectLoader trustMarkIssuerSubjectLoader) {

    /**
     * Validate content of configuration.
     * @throws IllegalArgumentException is thrown when configuration is missing
     */
    @PostConstruct
    public void validate() throws IllegalArgumentException {
      FederationAssert.assertNotEmpty(this.trustMarkId, "TrustMarkId is expected");
      FederationAssert.assertNotEmpty(this.trustMarkIssuerSubjectLoader, "TrustMarkIssuerSubjectLoader is expected");
      FederationAssert.assertNotEmpty(this.delegation, "Delegation can not be null");
      FederationAssert.assertNotEmpty(this.logoUri, "LogoUri can not be null");
      FederationAssert.assertNotEmpty(this.refUri, "RefUri can not be null");
    }

  }

}
