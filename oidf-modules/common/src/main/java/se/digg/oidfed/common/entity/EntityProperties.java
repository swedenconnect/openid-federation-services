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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Properties for a given entity.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class EntityProperties {
  private String alias;
  private String entityIdentifier;
  private List<String> authortyHints;
  private List<String> children;
  private Boolean isRoot;
  private JWK signKey;
  private JWKSet jwks;
  private String organizationName;

  /**
   * Default constructor.
   */
  public EntityProperties() {
  }

  /**
   * Constructor.
   * @param alias of the entity
   * @param entityIdentifier of the entity
   * @param authortyHints of the entity
   * @param children of the entity
   * @param isRoot if the entity is root or not
   * @param signKey of the entity
   * @param jwks of the entity
   * @param organizationName of the entity
   */
  public EntityProperties(final String alias, final String entityIdentifier, final List<String> authortyHints,
      final List<String> children,
      final Boolean isRoot, final JWK signKey, final JWKSet jwks, final String organizationName) {
    this.alias = alias;
    this.entityIdentifier = entityIdentifier;
    this.authortyHints = authortyHints;
    this.children = children;
    this.isRoot = isRoot;
    this.signKey = signKey;
    this.jwks = jwks;
    this.organizationName = organizationName;
  }
}
