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
package se.digg.oidfed.service.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import se.digg.oidfed.common.entity.PolicyRecord;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Properties for policy registry
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ConfigurationProperties("openid.federation.policy-registry")
public class PolicyConfigurationProperties {
  private List<PolicyRecordProperty> policies;

  /**
   * Validate configuration data
   */
  @PostConstruct
  public void validate() {
    Optional.ofNullable(this.policies).orElse(Collections.emptyList()).forEach(PolicyRecordProperty::validate);
  }

  /**
   * Record for an individual policy.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class PolicyRecordProperty {
    private Resource resource;
    private String name;

    /**
     * @return converted policy record
     */
    public PolicyRecord toRecord() {
      try {
        final String json = this.resource.getContentAsString(Charset.defaultCharset());
        final Map<String, Object> policy = (Map<String, Object>) new ObjectMapper().readValue(json, Map.class);
        return new PolicyRecord(this.name, policy);
      }
      catch (final Exception e) {
        throw new EntityConfigurationException("Policy parsing failed", e);
      }
    }

    /**
     * Validate configuration data
     */
    @PostConstruct
    public void validate() {
      FederationAssert.assertNotEmpty(this.resource,
          "openid.federation.policy-registry.policies[].resource is empty. Must be configured");
      FederationAssert.assertNotEmpty(this.name,
          "openid.federation.policy-registry.policies[].name is empty. Must be configured");

    }
  }
}

