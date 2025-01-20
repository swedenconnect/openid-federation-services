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
package se.digg.oidfed.service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.service.entity.EntityProperty;
import se.digg.oidfed.service.entity.PolicyConfigurationProperties;
import se.digg.oidfed.service.resolver.ResolverConfigurationProperties;
import se.digg.oidfed.service.trustanchor.TrustAnchorModuleProperties;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerModuleProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Properties for openid federation.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ConfigurationProperties("openid.federation")
public class OpenIdFederationConfigurationProperties {
  /** Name of sign keys used for this instance */
  private List<String> sign;

  /** Registry properties */
  @NestedConfigurationProperty
  private Registry registry;
  /** Module properties */
  @NestedConfigurationProperty
  private Modules modules;

  /** Entity configuration properties */
  @NestedConfigurationProperty
  private List<EntityProperty> entities;

  /** Policy configuration properties */
  @NestedConfigurationProperty
  private List<PolicyConfigurationProperties.PolicyRecordProperty> policies;

  /**
   * Registry properties for openid federation.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static final class Registry {
    @NestedConfigurationProperty
    private Integration integration;


    /**
     * IntegrationProperties for openid federation registry.
     *
     * @author Felix Hellman
     */
    @Setter
    @Getter
    public static final class Integration {
      private UUID instanceId;
      @NestedConfigurationProperty
      private Endpoints endpoints;
      private List<String> validationKeys;
      private List<Step> skipInit;

      /**
       * Checks if a step should be executed or not.
       * @param step to check
       * @return true if step should be executed, false if not
       */
      public boolean shouldExecute(final Step step) {
        return !this.skipInit.contains(step) && !this.skipInit.contains(Step.ALL);
      }

      /**
       * Endpoint properties for registry.
       *
       * @author Felix Hellman
       */
      @Setter
      @Getter
      public static final class Endpoints {
        private String basePath;
      }
    }

    /**
     * Steps to be loaded from entity registry.
     */
    public enum Step {
      /**
       * Submodule step
       */
      SUBMODULE,
      /**
       * Entity step
       */
      ENTITY,
      /**
       * Trust mark subject step
       */
      TRUST_MARK_SUBJECT,
      /**
       * Policy step
       */
      POLICY,
      /**
       * Skip all steps.
       */
      ALL
    }
  }

  /**
   * Module configuration for openid federation.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static final class Modules {
    @NestedConfigurationProperty
    private List<ResolverConfigurationProperties.ResolverModuleProperties> resolvers;
    @NestedConfigurationProperty
    private List<TrustAnchorModuleProperties.TrustAnchorSubModuleProperties> trustAnchors;
    @NestedConfigurationProperty
    private List<TrustMarkIssuerModuleProperties.TrustMarkIssuerSubModuleProperty> trustMarkIssuers;

    private String basePath;

    /**
     * @return list of all issuers
     */
    public List<String> getIssuers() {
      final List<String> issuers = new ArrayList<>();
      this.resolvers.stream()
          .map(ResolverConfigurationProperties.ResolverModuleProperties::getEntityIdentifier)
          .forEach(issuers::add);
      this.trustAnchors.stream()
          .map(TrustAnchorModuleProperties.TrustAnchorSubModuleProperties::getEntityIdentifier)
          .forEach(issuers::add);
      this.trustMarkIssuers.stream()
          .map(TrustMarkIssuerModuleProperties.TrustMarkIssuerSubModuleProperty::entityIdentifier)
          .forEach(issuers::add);
      return issuers;
    }
  }
}
