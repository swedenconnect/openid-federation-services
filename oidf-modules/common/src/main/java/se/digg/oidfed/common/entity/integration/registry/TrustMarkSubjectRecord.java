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
package se.digg.oidfed.common.entity.integration.registry;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.common.validation.FederationAssert;

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
 * @param trustMarkIssuer  issuer of the trust mark
 * @param trustMarkId  trust mark id of the trust mark
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Slf4j
@Builder(toBuilder = true)
public record TrustMarkSubjectRecord(
    String trustMarkSubject,
    String trustMarkIssuer,
    String trustMarkId,
    @Nullable Instant granted,
    @Nullable Instant expires,
    boolean revoked) implements Serializable {

  /**
   * Converting json structure to TrustMarkIssuerSubject object
   * Subject is mandatory and will be checked.
   *
   * @param record Reading subject,expires,granted,revoked values.
   * @return TrustMarkIssuerSubject filled with data
   */
  public static TrustMarkSubjectRecord fromJson(final Map<String, Object> record) {
    final TrustMarkSubjectRecord.TrustMarkSubjectRecordBuilder tmisBuilder = TrustMarkSubjectRecord.builder()
        .trustMarkSubject((String) record.get("trust_mark_subject"))
        .trustMarkIssuer((String) record.get("trust_mark_issuer"))
        .trustMarkId((String) record.get("trust_mark_id"));

    Optional.ofNullable((Boolean) record.get("revoked")).ifPresent(tmisBuilder::revoked);

    Optional.ofNullable((String) record.get("expires"))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::expires);

    Optional.ofNullable((String) record.get("granted"))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::granted);

    final TrustMarkSubjectRecord tms = tmisBuilder.build();
    tms.validate();
    return tms;
  }

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() {

    FederationAssert.assertNotEmpty(this.trustMarkSubject, "Subject is expected");
    if (Objects.nonNull(this.granted) && Objects.nonNull(this.expires)) {
      FederationAssert.assertTrue(this.granted.isBefore(this.expires), "Expires expected to be after " +
          "granted");
      if (this.expires.isBefore(Instant.now())) {
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'",
            this.trustMarkSubject, this.expires);
      }
    }
  }

  /**
   * @return this instance to json
   */
  public Map<String, Object> toJson() {
    final Map<String, Object> subject = Map.of(
        "subject", this.trustMarkSubject,
        "revoked", this.revoked,
        "issuer", this.trustMarkIssuer,
        "trustMarkId", this.trustMarkId
    );
    final HashMap<String, Object> json = new HashMap<>(subject);
    Optional.ofNullable(this.expires).ifPresent(exp -> json.put("expires", exp));
    Optional.ofNullable(this.granted).ifPresent(granted -> json.put("granted", granted));
    return json;
  }
}


