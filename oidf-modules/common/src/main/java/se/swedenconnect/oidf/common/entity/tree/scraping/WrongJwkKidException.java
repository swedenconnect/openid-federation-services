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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import java.util.Set;

/**
 * Thrown when the JWT header kid does not match any key in the entity configuration JWKS payload.
 *
 * @author Felix Hellman
 */
public class WrongJwkKidException extends RuntimeException {

  private final String entityId;
  private final String headerKid;
  private final Set<String> knownKids;

  /**
   * Constructor.
   *
   * @param entityId  the entity whose configuration has the mismatch
   * @param headerKid the kid found in the JWT header
   * @param knownKids the kids present in the JWKS payload
   */
  public WrongJwkKidException(final String entityId, final String headerKid, final Set<String> knownKids) {
    super("JWT header kid '%s' not in jwks for entity %s (known: %s)".formatted(headerKid, entityId, knownKids));
    this.entityId = entityId;
    this.headerKid = headerKid;
    this.knownKids = knownKids;
  }

  /**
   * @return entity identifier of the affected entity
   */
  public String getEntityId() {
    return this.entityId;
  }

  /**
   * @return the kid present in the JWT header
   */
  public String getHeaderKid() {
    return this.headerKid;
  }

  /**
   * @return the kids present in the JWKS payload
   */
  public Set<String> getKnownKids() {
    return this.knownKids;
  }
}
