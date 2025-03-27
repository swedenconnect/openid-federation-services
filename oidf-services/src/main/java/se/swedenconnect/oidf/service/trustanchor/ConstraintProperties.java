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
package se.swedenconnect.oidf.service.trustanchor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.NamingConstraints;

import java.util.List;

/**
 * Properties class for constraints.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class ConstraintProperties {

  /**
   * Default constructor.
   */
  public ConstraintProperties() {
  }

  /**
   * Constructor.
   *
   * @param maxPathLength
   * @param naming
   * @param allowedEntityTypes
   */
  public ConstraintProperties(
      final Long maxPathLength,
      final NamingConstraints naming,
      final List<String> allowedEntityTypes) {

    this.maxPathLength = maxPathLength;
    this.naming = naming;
    this.allowedEntityTypes = allowedEntityTypes;
  }

  private Long maxPathLength;
  @NestedConfigurationProperty
  private NamingConstraints naming;
  private List<String> allowedEntityTypes;
}
