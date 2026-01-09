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
package se.swedenconnect.oidf;

import com.nimbusds.jose.jwk.JWK;
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
import se.swedenconnect.oidf.routing.RouterProperties;

import java.util.List;
import java.util.UUID;

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

  @PostConstruct
  void validate() {
    Assert.notNull(this.resolver, "%s.%s can not be empty".formatted(PROPERTY_KEY, "resolver"));
    this.resolver.validate("%s.%s".formatted(PROPERTY_KEY, "resolver"));
    Assert.notNull(this.localRegistry, "%s.%s can not be empty".formatted(PROPERTY_KEY, "local-registry"));
    this.localRegistry.validate("%s.%s".formatted(PROPERTY_KEY, "local-registry"));
    Assert.notNull(this.routing, "%s.%s can not be empty".formatted(PROPERTY_KEY, "routing"));
    this.routing.validate("%s.%s".formatted(PROPERTY_KEY, "routing"));
  }
}
