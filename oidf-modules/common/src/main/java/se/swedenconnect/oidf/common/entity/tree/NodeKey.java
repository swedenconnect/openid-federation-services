/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.common.entity.tree;

import com.nimbusds.jwt.SignedJWT;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

/**
 * Class for representing a single node (key) in the resolver tree.
 *
 * @param entityId the entity identifier
 * @author Felix Hellman
 */
public record NodeKey(String entityId) {

  /**
   * Parses a string into a {@link NodeKey}
   *
   * @param key to parse
   * @return new instance
   */
  public static NodeKey parse(final String key) {
    return new NodeKey(key);
  }

  /**
   * @return string representation of key
   */
  public String getKey() {
    return this.entityId;
  }

  /**
   * Creates a key from a signed JWT.
   *
   * @param jwt to create key from
   * @return new instance
   */
  public static NodeKey fromSignedJwt(final SignedJWT jwt) {
    try {
      return new NodeKey(jwt.getJWTClaimsSet().getSubject());
    } catch (final Exception e) {
      throw new IllegalArgumentException("Failed to determine node key from signed jwt");
    }
  }

  /**
   * Creates a key from an entity record.
   *
   * @param record to create key from
   * @return new instance
   */
  public static NodeKey fromEntityRecord(final EntityRecord record) {
    return new NodeKey(record.getEntityIdentifier().getValue());
  }

  /**
   * @param record to compare
   * @return true if matches
   */
  public boolean matches(final EntityRecord record) {
    return this.entityId.equals(NodeKey.fromEntityRecord(record).entityId());
  }
}
