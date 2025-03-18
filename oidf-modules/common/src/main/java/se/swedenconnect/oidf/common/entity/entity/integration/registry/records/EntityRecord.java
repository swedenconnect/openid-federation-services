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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data class for entity record.
 *
 * @author Felix Hellman
 */
@Getter
@Builder
public class EntityRecord implements Serializable {
  private final EntityID issuer;
  private final EntityID subject;
  private final PolicyRecord policyRecord;
  private JWKSet jwks;
  private String overrideConfigurationLocation;
  private final HostedRecord hostedRecord;

  /**
   * Constructor.
   *
   * @param issuer                        of the entity
   * @param subject                       of the entity
   * @param policyRecord                  of the entity
   * @param jwks                          of the entity
   * @param overrideConfigurationLocation of the entity
   * @param hostedRecord                  optional parameter if the record is hosted
   */
  public EntityRecord(
      final EntityID issuer,
      final EntityID subject,
      final PolicyRecord policyRecord,
      final JWKSet jwks,
      final String overrideConfigurationLocation,
      final HostedRecord hostedRecord) {
    this.issuer = issuer;
    this.subject = subject;
    this.policyRecord = policyRecord;
    this.jwks = jwks;
    this.overrideConfigurationLocation = overrideConfigurationLocation;
    this.hostedRecord = hostedRecord;
  }

  /**
   * @return json object
   */
  public Map<String, Object> toJson() {
    final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

    builder.claim(RecordFields.Entity.ISSUER, this.issuer.getValue());
    builder.claim(RecordFields.Entity.SUBJECT, this.subject.getValue());
    builder.claim(RecordFields.Entity.POLICY_RECORD, this.policyRecord);

    Optional.ofNullable(this.hostedRecord)
        .ifPresent(record -> builder.claim(RecordFields.Entity.HOSTED_RECORD, record.toJson()));
    Optional.ofNullable(this.overrideConfigurationLocation).ifPresent(location -> builder.claim(
        RecordFields.Entity.OVERRIDE_CONFIGURATION_LOCATION, location));
    final JWTClaimsSet build = builder
        .build();
    return build.toJSONObject();
  }

  /**
   * @param entityRecord json to create instance from
   * @return instance of EntityRecord
   * @throws ParseException if parse failed
   */
  public static EntityRecord fromJson(final Map<String, Object> entityRecord) throws ParseException {
    final Optional<Object> hostedRecord = Optional.ofNullable(entityRecord.get(RecordFields.Entity.HOSTED_RECORD));
    return new EntityRecord(
        new EntityID((String) entityRecord.get(RecordFields.Entity.ISSUER)),
        new EntityID((String) entityRecord.get(RecordFields.Entity.SUBJECT)),
        PolicyRecord.fromJson((Map<String, Object>) entityRecord.get(RecordFields.Entity.POLICY_RECORD)),
        Optional.ofNullable(entityRecord.get(RecordFields.Entity.JWKS)).map(jwks -> {
          try {
            return JWKSet.parse((Map<String, Object>) jwks);
          } catch (final ParseException e) {
            throw new IllegalArgumentException("JWKS claim is not json claim", e);
          }
        }).orElse(null),
        Optional.ofNullable((String) entityRecord.get(RecordFields.Entity.OVERRIDE_CONFIGURATION_LOCATION))
            .orElse(null),
        hostedRecord.map(hr -> HostedRecord.fromJson((Map<String, Object>) hr))
            .orElse(null));
  }

  /**
   * @return true if hosted record is not null
   */
  public boolean isHosted() {
    return Objects.nonNull(this.getHostedRecord());
  }
}
