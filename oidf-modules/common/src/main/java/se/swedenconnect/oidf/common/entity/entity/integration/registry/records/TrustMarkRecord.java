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

import lombok.Builder;
import lombok.Getter;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Record class for trust mark.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
public class TrustMarkRecord implements Serializable {
  private final String trustMarkIssuerId;
  private final String trustMarkId;
  private final List<TrustMarkSubjectRecord> subjects;
  private final String logoUri;
  private final String ref;
  private final String delegation;

  /**
   * Constructor.
   * @param trustMarkIssuerId
   * @param trustMarkId
   * @param subjects
   * @param logoUri
   * @param ref
   * @param delegation
   */
  public TrustMarkRecord(final String trustMarkIssuerId,
                         final String trustMarkId,
                         final List<TrustMarkSubjectRecord> subjects,
                         final String logoUri,
                         final String ref,
                         final String delegation) {
    this.trustMarkIssuerId = trustMarkIssuerId;
    this.trustMarkId = trustMarkId;
    this.subjects = subjects;
    this.logoUri = logoUri;
    this.ref = ref;
    this.delegation = delegation;
  }

  /**
   * @return this record as json
   */
  public Map<String, Object> toJson() {
    final Map<String, Object> json = new HashMap<>();
    json.put(RecordFields.TrustMark.TRUST_MARK_ISSUER_ID, this.trustMarkIssuerId);
    json.put(RecordFields.TrustMark.TRUST_MARK_ID, this.trustMarkId);
    json.put(RecordFields.TrustMark.DELEGATION, this.delegation);
    json.put(RecordFields.TrustMark.REF, this.ref);
    json.put(RecordFields.TrustMark.LOGO_URI, this.logoUri);
    json.put(RecordFields.TrustMark.SUBJECTS, this.subjects.stream()
        .map(TrustMarkSubjectRecord::toJson)
        .toList());
    return json;
  }

  /**
   * @param json to convert from
   * @return converted record
   */
  public static TrustMarkRecord fromJson(final Map<String, Object> json) {
    final JsonObject jsonObject = new JsonObject(json);

    return TrustMarkRecord.builder()
        .trustMarkIssuerId(jsonObject.getStringValue(RecordFields.TrustMark.TRUST_MARK_ISSUER_ID))
        .trustMarkId(jsonObject.getStringValue(RecordFields.TrustMark.TRUST_MARK_ID))
        .delegation(jsonObject.getStringValue(RecordFields.TrustMark.DELEGATION))
        .ref(jsonObject.getStringValue(RecordFields.TrustMark.REF))
        .logoUri(jsonObject.getStringValue(RecordFields.TrustMark.LOGO_URI))
        .subjects(jsonObject.getObjectListClaim(RecordFields.TrustMark.SUBJECTS)
            .stream()
            .map(m -> (Map<String, Object>) m)
            .map(TrustMarkSubjectRecord::fromJson)
            .toList())
        .build();
  }

  /**
   * @return this record as property
   */
  public TrustMarkProperties toProperty() {
    return new TrustMarkProperties(
        new TrustMarkId(this.trustMarkId),
        Optional.ofNullable(this.logoUri),
        Optional.ofNullable(this.ref),
        Optional.ofNullable(this.delegation).map(TrustMarkDelegation::new),
        this.getSubjects()
    );
  }
}
