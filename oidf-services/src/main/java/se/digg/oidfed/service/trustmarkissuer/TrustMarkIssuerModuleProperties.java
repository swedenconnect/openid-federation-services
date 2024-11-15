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
package se.digg.oidfed.service.trustmarkissuer;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


/**
 * Properties for trust mark issuer.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@ConfigurationProperties(TrustMarkIssuerModuleProperties.PROPERTY_PATH)
public class TrustMarkIssuerModuleProperties {
  public static final String PROPERTY_PATH = "openid.federation.trust-mark-issuer";

  /** Set to true if this module should be active or not. */
  private Boolean active;

  private List<TrustMarkIssuers> trustMarkIssuers;

  /**
   * Validate content of the configuration
   */
  @PostConstruct
  public void validate(){
    FederationAssert.assertNotEmpty(trustMarkIssuers,
        "trustMarkIssuers is empty. Must be configured");

    trustMarkIssuers.forEach(TrustMarkIssuers::validate);
  }

  /**
   * TrustMark issuers
   *
   * @param remoteSubjectRepository Remote
   * @param trustMarkValidityDuration ValidityDuration of the TrustMArk JWT token.
   * @param issuerEntityId EntityId of this trustmark issuer
   * @param signKeyAlias Alias to the key that is used when signing this trustmark
   * @param trustMarks List of defined trustmarks
   * @param alias Alias name for this trust mark issuer
   */
  public record TrustMarkIssuers(
      String remoteSubjectRepository,
      Duration trustMarkValidityDuration,
      String issuerEntityId,
      String signKeyAlias,
      List<TrustMarkIssuer> trustMarks,
      String alias) {
    /**
     *  Validate content of the configuration
     */
    public void validate(){
      FederationAssert.assertNotEmpty(signKeyAlias,
          "SignKeyAlias is empty. Must be configured");
      FederationAssert.assertNotEmpty(trustMarks,
          "TrustMarks is empty. Must be configured");

      trustMarks.forEach(TrustMarkIssuer::validate);
    }

    /**
     * Definition of trustmark issuer
     *
     * @param trustMarkId The Trust Mark ID
     * @param logoUri Optional logo for issued Trust Marks
     * @param refUri Optional URL to information about issued Trust Marks
     * @param subjects trustMarkIssuerSubject
     * @param delegation TrustMark delegation
     */
    @Builder
    public record TrustMarkIssuer(
        TrustMarkId trustMarkId,
        String logoUri,
        String refUri,
        TrustMarkDelegation delegation,
        List<TrustMarkSubject> subjects) {
      /**
       * TrustMarkSubject
       * @param sub EntityId for Subject
       * @param granted Optional granted
       * @param expires Optional expires
       * @param revoked True is trust mark is revoked
       */
      public record TrustMarkSubject(
          String sub,
          Instant granted,
          Instant expires,
          boolean revoked) {
      }
      /**
       *  Validate content of the configuration
       */
      public void validate(){
        FederationAssert.assertNotEmpty(subjects,
            "trust-mark-issuers[].trust-marks[].subjects is empty. Must be configured");
      }
    }
  }
}
