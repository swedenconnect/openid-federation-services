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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Subject of an TrustMark
 *
 * @param sub     EntityId Subject
 * @param granted When subject is granted can be in the future
 * @param expires When subject expires, if left empty no expiredate will be used
 * @param revoked If this trust mark is revoked, no new trustmarks for this subject will be issued
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Slf4j
@Builder(toBuilder = true)
public record TrustMarkSubjectProperty(
    String sub,
    @Nullable Instant granted,
    @Nullable Instant expires,
    boolean revoked) implements Serializable {

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(this.sub, "Subject is expected");
    if (Objects.nonNull(this.granted) && Objects.nonNull(this.expires)) {
      FederationAssert.assertTrue(this.granted.isBefore(this.expires), "Expires expected to be after");
      if (this.expires.isBefore(Instant.now())) {
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'",
            this.sub, this.expires);
      }
    }
  }
}


