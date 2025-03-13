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

import lombok.Builder;
import lombok.Getter;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Record class for trust mark.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
public class TrustMarkRecord implements Serializable {
  /**
   * Json field key for issuer entity id.
   */
  public static final String ISSUER_ENTITY_IDENTIFIER_FIELD = "issuer-entity-identifier";
  /**
   * Json field key for trust mark entity id.
   */
  public static final String TRUST_MARK_ENTITY_ID_FIELD = "trust-mark-entity-id";
  /**
   * Json field key for delegation field.
   */
  public static final String DELEGATION_FIELD = "delegation";
  /**
   * Json field key for reference uri field.
   */
  public static final String REF_URI_FIELD = "ref-uri";
  /**
   * Json field key for logotype uri field.
   */
  public static final String LOGO_URI_FIELD = "logo-uri";
  /**
   * Json field key for subjects field.
   */
  public static final String SUBJECTS_FIELD = "subjects";
  private final String issuerEntityId;
  private final String trustMarkEntityId;
  private final List<TrustMarkSubjectRecord> subjects;
  private final String logoUri;
  private final String ref;
  private final String delegation;

  /**
   * Constructor.
   * @param issuerEntityId
   * @param trustMarkEntityId
   * @param subjects
   * @param logoUri
   * @param ref
   * @param delegation
   */
  public TrustMarkRecord(final String issuerEntityId,
                         final String trustMarkEntityId,
                         final List<TrustMarkSubjectRecord> subjects,
                         final String logoUri,
                         final String ref,
                         final String delegation) {
    this.issuerEntityId = issuerEntityId;
    this.trustMarkEntityId = trustMarkEntityId;
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
    json.put(ISSUER_ENTITY_IDENTIFIER_FIELD, this.issuerEntityId);
    json.put(TRUST_MARK_ENTITY_ID_FIELD, this.trustMarkEntityId);
    json.put(DELEGATION_FIELD, this.delegation);
    json.put(REF_URI_FIELD, this.ref);
    json.put(LOGO_URI_FIELD, this.logoUri);
    json.put(SUBJECTS_FIELD, this.subjects.stream().map(TrustMarkSubjectRecord::toJson).toList());
    return json;
  }

  /**
   * @param json to convert from
   * @return converted record
   */
  public static TrustMarkRecord fromJson(final Map<String, Object> json) {
    final JsonObject jsonObject = new JsonObject(json);

    return TrustMarkRecord.builder()
        .issuerEntityId(jsonObject.getStringValue(ISSUER_ENTITY_IDENTIFIER_FIELD))
        .trustMarkEntityId(jsonObject.getStringValue(TRUST_MARK_ENTITY_ID_FIELD))
        .delegation(jsonObject.getStringValue(DELEGATION_FIELD))
        .ref(jsonObject.getStringValue(REF_URI_FIELD))
        .logoUri(jsonObject.getStringValue(LOGO_URI_FIELD))
        .subjects(jsonObject.getObjectMapClaim(SUBJECTS_FIELD)
            .stream()
            .map(m -> (Map<String, Object>) m)
            .map(TrustMarkSubjectRecord::fromJson)
            .toList())
        .build();
  }
}
