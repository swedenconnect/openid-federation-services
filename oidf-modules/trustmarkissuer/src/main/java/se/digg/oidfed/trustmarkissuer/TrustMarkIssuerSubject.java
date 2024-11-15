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
 *  limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.time.Instant;
import java.util.Optional;

import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertTrue;

/**
 * Subject of an TrustMark
 *
 * @param sub EntityId Subject
 * @param granted When subject is granted can be in the future
 * @param expires When subject expires, if left empty no expiredate will be used
 * @param revoked If this trust mark is revoked, no new trustmarks for this subject will be issued
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Builder(toBuilder = true)
public record TrustMarkIssuerSubject(String sub, Optional<Instant> granted,
    Optional<Instant> expires, boolean revoked) {
  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(sub, "Subject is expected");
    FederationAssert.assertNotEmpty(granted, "Granted can not be null");
    FederationAssert.assertNotEmpty(expires, "Expires can not be null");

    if (granted.isPresent() && expires.isPresent()) {
      assertTrue(granted.get().isBefore(expires.get()), "Expires expected to be after granted");
      if (expires.get().isAfter(Instant.now())) {
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'", sub, expires.get());
      }
    }
  }
}


