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
import lombok.Getter;

import java.text.ParseException;
import java.util.Map;

/**
 * Record class for trust mark owner.
 *
 * @author Felix Hellman
 */
@Getter
public class TrustMarkOwnerRecord {
  private final String trustMarkId;
  private final String subject;
  private final JWKSet jwks;

  /**
   * @param trustMarkId of the trust mark in question
   * @param subject of the trust mark
   * @param jwks to validate the trust mark with
   */
  public TrustMarkOwnerRecord(final String trustMarkId, final String subject, final JWKSet jwks) {
    this.trustMarkId = trustMarkId;
    this.subject = subject;
    this.jwks = jwks;
  }

  /**
   * @return this record as json
   */
  public Map<String, Object> toJson() {
    return Map.of(
        "trust_mark_id", this.trustMarkId,
        "subject", this.subject,
        "jwks", this.jwks.toJSONObject(true)
    );
  }

  /**
   * Converts json object to instance.
   * @param json to read
   * @return new instance
   * @throws ParseException
   */
  public static TrustMarkOwnerRecord fromJson(final Map<String, Object> json) throws ParseException {
    final JWKSet parsed = JWKSet.parse((Map<String, Object>) json.get("jwks"));
    return new TrustMarkOwnerRecord(
        (String) json.get("trust_mark_id"),
        (String) json.get("subject"),
        parsed
    );
  }
}
