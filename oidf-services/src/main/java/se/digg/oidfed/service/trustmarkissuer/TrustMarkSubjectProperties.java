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
package se.digg.oidfed.service.trustmarkissuer;

import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkSubjectRecord;

import java.time.Instant;

/**
 * TrustMarkSubject
 *
 * @param sub     EntityId for Subject
 * @param granted Optional granted
 * @param expires Optional expires
 * @param revoked True is trust mark is revoked
 * @param iss     issuer of the trust mark
 * @param tmi     trust mark id of the trust mark
 * @author Felix Hellman
 * @author Per Fredrik Plars
 */
public record TrustMarkSubjectProperties(
    String sub,
    String iss,
    String tmi,
    Instant granted,
    Instant expires,
    boolean revoked) {
  /**
   * Converts to TrustMarkSubject
   *
   * @return new instance
   */
  public TrustMarkSubjectRecord toSubject() {
    return new TrustMarkSubjectRecord(this.sub, this.granted, this.expires, this.revoked);
  }
}
