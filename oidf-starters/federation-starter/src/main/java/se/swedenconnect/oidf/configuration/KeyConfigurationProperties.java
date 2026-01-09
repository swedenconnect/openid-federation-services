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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.annotation.Order;
import se.swedenconnect.oidf.KeyEntry;

import java.util.List;

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
}
