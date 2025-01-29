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
package se.digg.oidfed.service.trustmarkissuer;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import se.digg.oidfed.common.validation.FederationAssert;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuerProperties;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubject;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


/**
 * Properties for trust mark issuer.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
public class TrustMarkIssuerModuleProperties {
  public static final String PROPERTY_PATH = "openid.federation.trust-mark-issuer";

  /**
   * Set to true if this module should be active or not.
   */
  private Boolean active;
  /**
   * Rest client to use for entity registry
   */
  private String client;

  /**
   * Alias of all keys that can verify trustmarksubject records
   */
  private List<String> jwkAlias;


  /**
   * TrustmarkIssuers
   */
  private List<TrustMarkIssuerSubModuleProperty> trustMarkIssuers;

  /**
   * Validate data of configuration
   */
  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(this.trustMarkIssuers,
        "trustMarkIssuers is empty. Must be configured");

    this.trustMarkIssuers.forEach(TrustMarkIssuerSubModuleProperty::validate);
  }

  /**
   * TrustMark issuers
   *
   * @param remoteSubjectRepositoryJwtTrustKeyAlias Trust key when verify signature of trustmark register JWT token
   * @param trustMarkValidityDuration               ValidityDuration of the TrustMark JWT token. Default value is PT30M
   * @param entityIdentifier                        EntityId of this trustmark issuer
   * @param trustMarks                              List of defined trustmarks
   * @param alias                                   Alias name for this trust mark issuer
   */
  public record TrustMarkIssuerSubModuleProperty(
      String alias,
      String entityIdentifier,

      String remoteSubjectRepositoryJwtTrustKeyAlias,
      @DefaultValue("PT30M") Duration trustMarkValidityDuration,
      List<TrustMarkProperties> trustMarks) {
    /**
     * Validate content of the configuration
     */
    public void validate() {
      FederationAssert.assertNotEmpty(this.trustMarks,
          "TrustMarks is empty. Must be configured");

      this.trustMarks.forEach(TrustMarkProperties::validate);
    }

    /**
     * Converts to properties.
     * @return new instance
     */
    public TrustMarkIssuerProperties toProperties() {
      return new TrustMarkIssuerProperties(this.trustMarkValidityDuration, new EntityID(this.entityIdentifier),
          this.trustMarks.stream().map(TrustMarkProperties::toProperties).toList(),
          this.alias);
    }

    /**
     * Definition of trustmark issuer
     *
     * @param trustMarkId The Trust Mark ID
     * @param logoUri     Optional logo for issued Trust Marks
     * @param refUri      Optional URL to information about issued Trust Marks
     * @param subjects    trustMarkIssuerSubject
     * @param delegation  TrustMark delegation
     */
    @Builder
    public record TrustMarkProperties(
        TrustMarkId trustMarkId,
        String logoUri,
        String refUri,
        TrustMarkDelegation delegation,
        List<TrustMarkSubjectProperties> subjects) {
      /**
       * TrustMarkSubject
       *
       * @param sub     EntityId for Subject
       * @param granted Optional granted
       * @param expires Optional expires
       * @param revoked True is trust mark is revoked
       */
      public record TrustMarkSubjectProperties(
          String sub,
          Instant granted,
          Instant expires,
          boolean revoked) {
        /**
         * Converts to TrustMarkSubject
         * @return new instace
         */
        public TrustMarkSubject toSubject() {
          return new TrustMarkSubject(this.sub, this.granted, this.expires, this.revoked);
        }
      }

      /**
       * Validate content of the configuration
       */
      public void validate() {
        FederationAssert.assertNotEmpty(this.subjects,
            "trust-mark-issuers[].trust-marks[].subjects is empty. Must be configured");
      }

      /**
       * Converts to properties
       * @return new instance
       */
      public TrustMarkIssuerProperties.TrustMarkProperties toProperties() {
        return new TrustMarkIssuerProperties.TrustMarkProperties(this.trustMarkId, Optional.ofNullable(this.logoUri),
            Optional.ofNullable(this.refUri),
            Optional.ofNullable(this.delegation));
      }
    }
  }
}
