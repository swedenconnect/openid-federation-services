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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TrustMarkIssuer module from registry.
 *
 * @author Felix Hellman
 */
@Getter
@Builder
public class TrustMarkIssuerModuleResponse {
  private Duration trustMarkValidityDuration;
  private String entityIdentifier;
  private List<TrustMarkResponse> trustMarks;
  private Boolean active;

  /**
   * Constructor.
   *
   * @param trustMarkValidityDuration duration for a given trust mark
   * @param entityIdentifier          of the trust mark issuer
   * @param trustMarks                list of trust marks
   * @param active                    true if module is active
   */
  public TrustMarkIssuerModuleResponse(
      final Duration trustMarkValidityDuration,
      final String entityIdentifier,
      final List<TrustMarkResponse> trustMarks,
      final Boolean active) {

    this.trustMarkValidityDuration = trustMarkValidityDuration;
    this.entityIdentifier = entityIdentifier;
    this.trustMarks = trustMarks;
    this.active = active;
  }

  /**
   * Converts json object {@link java.util.HashMap} to new instance
   *
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    return TrustMarkIssuerModuleResponse.builder()
        .trustMarkValidityDuration(Duration.parse((String) json.get("trust-mark-token-validity-duration")))
        .entityIdentifier((String) json.get("entity-identifier"))
        .trustMarks(Optional.ofNullable((List<Map<String, Object>>) json.get("trust-marks"))
            .map(strings -> strings.stream().map(TrustMarkResponse::fromJson).toList()).orElse(
                Collections.emptyList()))
        .active((Boolean) json.get("active"))
        .build();
  }

  /**
   * @return properties
   */
  public TrustMarkIssuerProperties toProperties() {
    return new TrustMarkIssuerProperties(
        this.trustMarkValidityDuration,
        new EntityID(this.entityIdentifier),
        this.trustMarks.stream().map(TrustMarkResponse::toProperty).toList()
    );
  }

  /**
   * @return this instance as json
   */
  public Map<String, Object> toJson() {
    return Map.of(
        "trust-mark-validity-duration", this.trustMarkValidityDuration,
        "entity-identifier", this.entityIdentifier,
        "trust-marks", this.trustMarks.stream().map(TrustMarkResponse::toJson).toList()
    );
  }

  /**
   * @param trustMarkId
   * @param logoUri
   * @param refUri
   * @param delegation
   */
  @Builder
  public record TrustMarkResponse(TrustMarkId trustMarkId, Optional<String> logoUri, Optional<String> refUri,
                                  Optional<TrustMarkDelegation> delegation,
                                  List<TrustMarkSubjectRecord> trustMarkSubjectRecords) {
    /**
     * @return convert this instance to properties
     */
    public TrustMarkIssuerProperties.TrustMarkProperties toProperty() {
      return new TrustMarkIssuerProperties.TrustMarkProperties(
          this.trustMarkId,
          this.logoUri,
          this.refUri,
          this.delegation,
          this.trustMarkSubjectRecords
      );
    }

    /**
     * Converts a JSON representation in the form of a map to a {@code TrustMarkResponse} object.
     *
     * @param json a map containing the JSON data with keys "trust-mark-entity-id", "logo-uri",
     *             "ref-uri", and "delegation", where values are expected to be of appropriate type
     *             (e.g., String for most keys).
     * @return a {@code TrustMarkResponse} instance populated with data from the provided map.
     */
    public static TrustMarkResponse fromJson(final Map<String, Object> json) {
      final List<TrustMarkSubjectRecord> trustMarkSubjects =
          Optional.ofNullable((List<Map<String, Object>>) json.get("trust-mark-subjects"))
              .map(claim -> claim.stream().map(TrustMarkSubjectRecord::fromJson).toList()).orElse(List.of());
      return TrustMarkResponse.builder()
          .trustMarkId(new TrustMarkId((String) json.get("trust-mark-entity-id")))
          .logoUri(Optional.ofNullable((String) json.get("logo-uri")))
          .refUri(Optional.ofNullable((String) json.get("ref-uri")))
          .delegation(Optional.ofNullable((String) json.get("delegation")).map(TrustMarkDelegation::new))
          .trustMarkSubjectRecords(trustMarkSubjects)
          .build();
    }

    /**
     * @return this instance as json
     */
    public Map<String, Object> toJson() {
      return Map.of(
          "trust-mark-entity-id", this.trustMarkId,
          "logo-uri", this.logoUri,
          "ref-uri", this.refUri,
          "delegation", this.delegation,
          "trust-mark-subjects", this.trustMarkSubjectRecords.stream()
              .map(TrustMarkSubjectRecord::toJson)
              .toList()
      );
    }
  }
}
