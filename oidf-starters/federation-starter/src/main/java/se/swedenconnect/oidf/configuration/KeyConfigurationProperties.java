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
package se.swedenconnect.oidf.configuration;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.annotation.Order;
import se.swedenconnect.oidf.KeyEntry;
import se.swedenconnect.oidf.common.entity.keys.KeyProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration properties for keys.
 *
 * @author Felix Hellman
 */
@Order(Integer.MAX_VALUE)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
@ConfigurationProperties("federation.keys")
public class KeyConfigurationProperties {
  /**
   * Key id algorithm types.
   */
  public enum KeyIdAlgorithmType {
    DEFAULT,
    SERIAL
  }


  /**
   * Key id algorithm
   */
  private KeyIdAlgorithmType kidAlgorithm;

  /**
   * Additional Keys
   */
  @NestedConfigurationProperty
  private List<KeyEntry> additionalKeys = List.of();

  private Map<String, List<String>> mapping;

  /**
   * Get mapping of key
   * @param property to check
   * @return key mapping
   */
  public String getMapping(final KeyProperty property) {
    final String kid = property.getKey().getKeyID();
    final String alias = property.getAlias();
    if (this.isMappedKey(kid, alias, "federation")) {
      return "federation";
    }
    if (this.isMappedKey(kid, alias, "hosted")) {
      return "hosted";
    }
    return "alias";
  }

  private boolean isMappedKey(final String kid, final String alias, final String mapping) {
    return this.getMapping().get(mapping).contains(kid) ||
           this.getMapping().get(mapping).contains(alias);
  }

  @PostConstruct
  void validate() {
    if (Objects.nonNull(this.mapping)) {
      if (!this.mapping.containsKey("hosted")) {
        log.warn("No hosted key was specified for this instance, no default key will be loaded.");
      }
      if (!this.mapping.containsKey("federation")) {
        throw new IllegalArgumentException("No federation key was specified which is required. See documentation.");
      }
    } else {
      throw new IllegalArgumentException("No key mapping was configured which is required. See documentation.");
    }
  }
}
