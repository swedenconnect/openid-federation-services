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

import lombok.Getter;
import lombok.Setter;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.keys.KeyRegistry;

import java.util.List;

/**
 * Configuration property to be loaded from spring.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class SpringEntityProperty {
  private String path;
  private String entityIdentifier;
  private List<String> authorityHints;
  private Boolean isRoot;
  private String signKeyAlias;
  private List<String> jwkAlias;
  private String organizationName;

  /**
   * Converts the properties to the correct format.
   * @param registry to load keys from
   * @return new instance
   */
  public EntityProperties toEntityProperties(final KeyRegistry registry) {
    return new EntityProperties(
        path,
        entityIdentifier,
        authorityHints,
        isRoot,
        registry.getKey(signKeyAlias),
        registry.getSet(jwkAlias),
        organizationName);
  }
}
