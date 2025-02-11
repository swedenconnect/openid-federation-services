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
package se.digg.oidfed.common.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

/**
 * Class for representing a single node (key) in the resolver tree.
 * @param issuer part
 * @param subject part
 *
 * @author Felix Hellman
 */
public record NodeKey(String issuer, String subject) {
  /**
   * Parses a string into a {@link NodeKey}
   * @param key to parse
   * @return new instance
   */
  public static NodeKey parse(final String key) {
    final String[] split = key.split("!");
    return new NodeKey(split[0], split[1]);
  }

  /**
   * @return string representation of key
   */
  public String getKey() {
    return "%s!%s".formatted(this.issuer, this.subject);
  }

  /**
   * Creates a key from an entity statement.
   * @param es to create key from
   * @return new instance
   */
  public static NodeKey fromEntityStatement(final EntityStatement es) {
    return new NodeKey(
        es.getClaimsSet().getIssuer().getValue(),
        es.getClaimsSet().getSubject().getValue()
    );
  }
}
