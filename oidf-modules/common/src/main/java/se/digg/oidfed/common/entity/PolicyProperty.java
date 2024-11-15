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
package se.digg.oidfed.common.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Holds multiple policies.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class PolicyProperty {
  private List<PolicyRecord> policies;

  /**
   * Record class of an individual policy.
   *
   * @author Felix Hellman
   */
  @Getter
  @Setter
  public static class PolicyRecord {

    /**
     * Constructor.
     * @param name of the policy
     * @param policy object of the policy
     */
    public PolicyRecord(final String name, final Map<String, Object> policy) {
      this.name = name;
      this.policy = policy;
    }

    private String name;
    private Map<String, Object> policy;
  }
}
