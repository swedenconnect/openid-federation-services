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
package se.swedenconnect.oidf.service.router.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * TrustMarkStatusReply indicates if trustmark is active or not
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustMarkStatusReply {
  @JsonProperty("active")
  private Boolean active;

  /**
   * Default constructor.
   */
  public TrustMarkStatusReply() {
  }

  /**
   * Constructor.
   * @param active true or false
   */
  public TrustMarkStatusReply(final Boolean active) {
    this.active = active;
  }
}
