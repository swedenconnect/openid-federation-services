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
package se.digg.oidfed.common.entity.integration.registry.records;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Builder;
import lombok.Getter;
import se.digg.oidfed.common.entity.integration.properties.TrustMarkIssuerProperties;

import java.io.Serializable;
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
public class TrustMarkIssuerModuleRecord implements Serializable {

  private Duration trustMarkValidityDuration;
  private String entityIdentifier;
  private List<TrustMarkRecord> trustMarks;

  /**
   * Constructor.
   *
   * @param trustMarkValidityDuration duration for a given trust mark
   * @param entityIdentifier          of the trust mark issuer
   * @param trustMarks                list of trust marks
   */
  public TrustMarkIssuerModuleRecord(
      final Duration trustMarkValidityDuration,
      final String entityIdentifier,
      final List<TrustMarkRecord> trustMarks) {

    this.trustMarkValidityDuration = trustMarkValidityDuration;
    this.entityIdentifier = entityIdentifier;
    this.trustMarks = trustMarks;
  }
  /**
   * Converts json object {@link java.util.HashMap} to new instance
   *
   * @param json to read
   * @return new instance
   */
  public static TrustMarkIssuerModuleRecord fromJson(final Map<String, Object> json) {
    return TrustMarkIssuerModuleRecord.builder()
        .trustMarkValidityDuration(Duration.parse((String)
            json.get(RecordFields.TrustMarkIssuerModule.TRUST_MARK_TOKEN_VALIDITY_DURATION)))
        .entityIdentifier((String) json.get(RecordFields.TrustMarkIssuerModule.ENTITY_IDENTIFIER))
        .trustMarks(Optional.ofNullable((List<Map<String, Object>>)
                json.get(RecordFields.TrustMarkIssuerModule.TRUST_MARKS))
            .map(strings -> strings.stream().map(TrustMarkRecord::fromJson).toList()).orElse(
                Collections.emptyList()))
        .build();
  }

  /**
   * @return convert this instance to properties
   */
  public TrustMarkIssuerProperties toProperties() {
    return new TrustMarkIssuerProperties(
        this.trustMarkValidityDuration,
        new EntityID(this.entityIdentifier),
        this.trustMarks.stream().map(TrustMarkRecord::toProperty).toList()
    );
  }

  /**
   * @return this instance as json
   */
  public Map<String, Object> toJson() {
    return Map.of(
        RecordFields.TrustMarkIssuerModule.TRUST_MARK_VALIDITY_DURATION, this.trustMarkValidityDuration,
        RecordFields.TrustMarkIssuerModule.ENTITY_IDENTIFIER, this.entityIdentifier,
        RecordFields.TrustMarkIssuerModule.TRUST_MARKS, this.trustMarks.stream().map(TrustMarkRecord::toJson).toList()
    );
  }
}
