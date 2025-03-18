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
package se.swedenconnect.oidf.service.trustmarkissuer;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.bind.DefaultValue;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


/**
 * Properties for trust mark issuer.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
public class TrustMarkIssuerModuleConfigurationProperties {
  /**
   * For this proeprty.
   */
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
   * Alias of all keys that can verify trustmarksubject trustMarkSubjects
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
   */
  public record TrustMarkIssuerSubModuleProperty(
      String entityIdentifier,

      String remoteSubjectRepositoryJwtTrustKeyAlias,
      @DefaultValue("PT30M") Duration trustMarkValidityDuration,
      List<TrustMarkConfigurationProperties> trustMarks) {
    /**
     * Validate content of the configuration
     */
    public void validate() {
      FederationAssert.assertNotEmpty(this.trustMarks,
          "TrustMarks is empty. Must be configured");

      this.trustMarks.forEach(TrustMarkConfigurationProperties::validate);
    }

    /**
     * Converts to properties.
     * @return new instance
     */
    public TrustMarkIssuerProperties toProperties() {
      return new TrustMarkIssuerProperties(this.trustMarkValidityDuration, new EntityID(this.entityIdentifier),
          this.trustMarks.stream().map(TrustMarkConfigurationProperties::toProperties).toList());
    }

    /**
     * Definition of trustmark issuer
     *
     * @param trustMarkId The Trust Mark ID
     * @param logoUri     Optional logo for issued Trust Marks
     * @param refUri      Optional URL to information about issued Trust Marks
     * @param trustMarkSubjects for this trust mark
     * @param delegation  TrustMark delegation
     */
    @Builder
    public record TrustMarkConfigurationProperties(
        TrustMarkId trustMarkId,
        String logoUri,
        String refUri,
        TrustMarkDelegation delegation,
        List<TrustMarkSubjectRecord> trustMarkSubjects) {

      /**
       * Validate content of the configuration
       */
      public void validate() {

      }

      /**
       * Converts to properties
       * @return new instance
       */
      public TrustMarkProperties toProperties() {
        return new TrustMarkProperties(this.trustMarkId, Optional.ofNullable(this.logoUri),
            Optional.ofNullable(this.refUri),
            Optional.ofNullable(this.delegation),
            Optional.ofNullable(this.trustMarkSubjects)
                .orElse(List.of()));
      }
    }
  }
}
