/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.routing.RouterProperties;

import java.util.List;
import java.util.Objects;

/**
 * Properties for OpenId Federation.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Validated
@ConfigurationProperties(FederationProperties.PROPERTY_KEY)
public class FederationProperties {


  private ResolverConfigurationProperties resolver;

  /**
   * Client for the outbound requests that are made while serving an inbound request, see
   * {@link RestClientProperty#requestPath()}. It is separate from the client of the tree scraping, so that a burst
   * of inbound requests cannot exhaust the connections the scraping needs, and so that the two can be given their
   * own timeouts. Its trust store defaults to the one of {@code resolver.client}, since both talk to the same peers.
   */
  @NestedConfigurationProperty
  private RestClientProperty requestClient = RestClientProperty.requestPath();

  /**
   * Property key for this configuration property.
   */
  public static final String PROPERTY_KEY = "federation";

  /**
   * Registry properties
   */
  @NestedConfigurationProperty
  private RegistryConfigurationProperties registry;

  /**
   * Local registry settings.
   */
  @NestedConfigurationProperty
  private PropertyRegistry localRegistry;

  @Nonnull
  @NestedConfigurationProperty
  private RouterProperties routing;

  /**
   * Whether {@code ec_location} values may use the {@code http} scheme. OpenID Federation Entity Configuration
   * Hosting 1.0 only allows {@code https} and data URLs, so this is only meant for local test and demo setups.
   * Defaults to {@code false}.
   */
  private boolean allowHttpEcLocation;

  @PostConstruct
  void validate() {
    Assert.notNull(this.resolver, "%s.%s can not be empty".formatted(PROPERTY_KEY, "resolver"));
    this.resolver.validate("%s.%s".formatted(PROPERTY_KEY, "resolver"));
    Assert.notNull(this.requestClient, "%s.%s can not be empty".formatted(PROPERTY_KEY, "request-client"));
    Assert.isNull(this.requestClient.getBaseUri(),
        "%s.%s can not be set".formatted(PROPERTY_KEY, "request-client.base-uri"));
    if (this.requestClient.getTrustStoreBundleName() == null) {
      this.requestClient.setTrustStoreBundleName(this.resolver.getClient().getTrustStoreBundleName());
    }
    this.requestClient.validateClient("%s.%s".formatted(PROPERTY_KEY, "request-client"));
    if (Objects.nonNull(this.localRegistry)) {
      this.localRegistry.validate("%s.%s".formatted(PROPERTY_KEY, "local-registry"),
          new EcLocationValidator(this.allowHttpEcLocation));
    } else {
      this.localRegistry = new PropertyRegistry(
          List.of(),
          List.of(),
          List.of(),
          List.of()
      );
    }
    Assert.notNull(this.routing, "%s.%s can not be empty".formatted(PROPERTY_KEY, "routing"));
    this.routing.validate("%s.%s".formatted(PROPERTY_KEY, "routing"));
  }
}
