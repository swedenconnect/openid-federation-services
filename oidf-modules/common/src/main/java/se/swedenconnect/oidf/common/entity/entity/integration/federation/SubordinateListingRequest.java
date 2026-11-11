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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Subordinate listing request (OpenID Federation 1.0, Section 8.2.1).
 *
 * @param entityType the {@code entity_type} values, or null
 * @param trustMarked the {@code trust_marked} value, or null
 * @param trustMarkType the {@code trust_mark_type} value, or null
 * @param intermediate the {@code intermediate} value, or null
 * @author Felix Hellman
 */
public record SubordinateListingRequest(List<String> entityType, Boolean trustMarked, String trustMarkType,
    Boolean intermediate) implements Serializable {

  /**
   * @return request without any filters set
   */
  public static SubordinateListingRequest requestAll() {
    return new SubordinateListingRequest(null, null, null, null);
  }

  /**
   * @return true if any parameter is set
   */
  public boolean requiresFiltering() {
    if (Objects.nonNull(this.entityType) && !this.entityType.isEmpty()) {
      return true;
    }
    // trust_marked and intermediate only filter when true (Section 8.2.1)
    return Objects.nonNull(this.trustMarkType)
        || Boolean.TRUE.equals(this.trustMarked)
        || Boolean.TRUE.equals(this.intermediate);
  }
}
