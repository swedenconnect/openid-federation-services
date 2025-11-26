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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Request class for fetching entity configuration.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
public final class EntityConfigurationRequest implements Serializable {
  private final EntityID entityID;
  private final String ecLocation;

  /**
   * @param entityID to fetch
   */
  public EntityConfigurationRequest(final EntityID entityID) {
    this.entityID = entityID;
    this.ecLocation = null;
  }

  /**
   * @param entityID to fetch
   * @param ecLocation optional location for entity configuration
   */
  public EntityConfigurationRequest(final EntityID entityID, final String ecLocation) {
    this.entityID = entityID;
    this.ecLocation = ecLocation;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final EntityConfigurationRequest that = (EntityConfigurationRequest) obj;
    return Objects.equals(this.entityID, that.entityID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.entityID);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("EntityConfigurationRequest{");
    sb.append("entityID=").append(this.entityID);
    sb.append(", ecLocation='").append(this.ecLocation).append('\'');
    sb.append('}');
    return sb.toString();
  }
}