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
package se.swedenconnect.oidf.service.entity;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;
import se.swedenconnect.oidf.service.JsonObjectProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Properties for policy registry
 *
 * @author Felix Hellman
 */
@Getter
@Setter
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
    private JsonObjectProperty policy;
    private String id;

    /**
     * @return converted policy record
     */
    public PolicyRecord toRecord() {
      return new PolicyRecord(this.id, this.policy.toJsonObject());
    }

    /**
     * Validate configuration data
     */
    @PostConstruct
    public void validate() {

      FederationAssert.assertNotEmpty(this.policy,
          "openid.federation.policy-registry.policies[].policy is empty. Must be configured");
      FederationAssert.assertNotEmpty(this.id,
          "openid.federation.policy-registry.policies[].name is empty. Must be configured");
    }
  }
}

