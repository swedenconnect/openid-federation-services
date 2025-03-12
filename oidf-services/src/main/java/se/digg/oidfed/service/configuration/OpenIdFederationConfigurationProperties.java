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
package se.digg.oidfed.service.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.service.entity.EntityProperty;
import se.digg.oidfed.service.entity.PolicyConfigurationProperties;
import se.digg.oidfed.service.resolver.ResolverConfigurationProperties;
import se.digg.oidfed.service.router.RouterProperties;
import se.digg.oidfed.service.trustanchor.TrustAnchorModuleProperties;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerModuleProperties;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkSubjectProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  /**
   * Name of sign keys used for this instance
   */
  private List<String> sign;

  /**
   * Registry properties
   */
  @NestedConfigurationProperty
  private Registry registry;
  /**
   * Module properties
   */
  @NestedConfigurationProperty
  private Modules modules;

  /**
   * Entity configuration properties
   */
  @NestedConfigurationProperty
  private List<EntityProperty> entities;

  /**
   * Policy configuration properties
   */
  @NestedConfigurationProperty
  private List<PolicyConfigurationProperties.PolicyRecordProperty> policies;

  @NestedConfigurationProperty
  private List<TrustMarkSubjectProperties> trustMarkSubjects;

  /**
   * Properties for internal routing.
   */
  @NestedConfigurationProperty
  private RouterProperties routerProperties;

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
      private Boolean enabled;
      @NestedConfigurationProperty
      private Endpoints endpoints;
      private List<String> validationKeys;

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
  }
}
