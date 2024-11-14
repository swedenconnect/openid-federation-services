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
package se.digg.oidfed.service.keys;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import se.digg.oidfed.common.keys.KeyProperty;

import java.util.List;

/**
 * Configuration properties for key-registry.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@ConfigurationProperties("openid.federation.key-registry")
public class KeyConfigurationProperties {

  /** Key properties to add to the registry */
  @NestedConfigurationProperty
  private List<KeyProperty> keys;
}
