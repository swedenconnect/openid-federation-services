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
package se.digg.oidfed.service.resolver;

import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.common.validation.FederationAssert;
import se.digg.oidfed.resolver.ResolverProperties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Configuration properties for Resolver.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class ResolverConfigurationProperties {

  /**
   * Property path
   */
  public static final String PROPERTY_PATH = "openid.federation.resolver";


  /**
   * List of Resolver Module Properties
   */
  @NestedConfigurationProperty
  private List<ResolverModuleProperties> resolvers;
  /**
   * Name of the rest client to use
   */
  private String client;
  /**
   * Set to true if this module should be active or not.
   */
  private Boolean active;


  /**
   * Validate data of configuration
   */
  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(this.resolvers,
        "resolvers is empty. Must be configured");

    this.resolvers.forEach(ResolverModuleProperties::validate);
  }

  /**
   * Configuration properties for each resolver module.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class ResolverModuleProperties {

    /**
     * Supported trustAnchor for this resolver
     */
    private String trustAnchor;

    /**
     * Duration for resolve responses
     */
    private Duration duration = Duration.of(7, ChronoUnit.DAYS);

    private List<String> trustedKeys;

    private String signKeyAlias;

    private String entityIdentifier;

    private String alias;

    /**
     * @param registry to load keys from
     * @return properties
     */
    public ResolverProperties toResolverProperties(final KeyRegistry registry) {
      final List<JWK> list = this.trustedKeys.stream()
          .map(registry::getKey)
          .map(Optional::orElseThrow)
          .toList();

      return new ResolverProperties(
          this.trustAnchor,
          this.duration,
          list,
          this.entityIdentifier,
          Duration.ofSeconds(10),
          this.alias
      );
    }

    /**
     * Validate configuration data
     */
    public void validate() {

      FederationAssert.assertNotEmpty(this.trustAnchor,
          "trustAnchor is empty. Must be configured");
    }

  }
}
