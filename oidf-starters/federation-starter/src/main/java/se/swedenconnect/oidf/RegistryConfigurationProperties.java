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

import com.nimbusds.jose.jwk.JWKSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * Registry properties for openid federation.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistryConfigurationProperties {

  private Integration integration;

  /**
   * IntegrationProperties for openid federation registry.
   *
   * @author Felix Hellman
   */
  @Setter
  @Getter
  public static final class Integration {
    @NestedConfigurationProperty
    private RestClientProperty client;
    private UUID instanceId;
    private Boolean enabled;
    private JWKSet validationKeys;

    /**
     * If enabled properties are validated
     */
    public void validate() {
      if (this.enabled == null || !this.enabled) {
        return;
      }
      final String basePropName = "openid.federation.registry.integration.";
      Assert.notNull(this.instanceId, basePropName + "instance-id must be configured");
      Assert.notNull(this.validationKeys, basePropName + "validationKeys must be configured");
      Assert.isTrue(!this.validationKeys.isEmpty(), basePropName + "validationKeys must be configured");
      Assert.notNull(this.client, "Rest client properties can not be null");
    }
  }
}

