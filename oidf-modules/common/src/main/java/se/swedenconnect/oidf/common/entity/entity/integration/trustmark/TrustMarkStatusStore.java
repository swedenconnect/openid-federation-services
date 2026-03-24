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
package se.swedenconnect.oidf.common.entity.entity.integration.trustmark;

import com.nimbusds.jwt.SignedJWT;

import java.util.Optional;

/**
 * Store for persisting trust mark status results obtained during tree build.
 *
 * @author Felix Hellman
 */
public interface TrustMarkStatusStore {

  /**
   * Records the active/inactive status for a specific (subject, trust mark type) pair.
   *
   * @param subject       entity ID of the subject
   * @param trustMarkType trust mark type identifier
   * @param trustMarkStatus trust mark status
   */
  void setTrustMarkStatus(String subject, String trustMarkType, TrustMarkStatusResponse trustMarkStatus);

  /**
   * Retrieves the stored status for a (subject, trust mark type) pair.
   *
   * @param subject       entity ID of the subject
   * @param trustMarkType trust mark type identifier
   * @return the stored status, or empty if no status has been recorded
   */
  Optional<TrustMarkStatusResponse> getTrustMarkStatus(String subject, String trustMarkType);

  /**
   * Clears all stored statuses.
   */
  void clear();
}
