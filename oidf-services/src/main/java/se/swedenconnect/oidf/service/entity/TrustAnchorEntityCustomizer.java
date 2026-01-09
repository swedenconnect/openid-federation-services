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
package se.swedenconnect.oidf.service.entity;

import com.nimbusds.jwt.JWTClaimsSet;
import se.swedenconnect.oidf.common.entity.entity.EntityConfigurationClaimCustomizer;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.HashMap;
import java.util.Optional;

/**
 * EntityConfigurationClaimCustomizer for Trust Anchors additional claims.
 *
 * @author Felix Hellman
 */
public class TrustAnchorEntityCustomizer implements EntityConfigurationClaimCustomizer {

  private final CompositeRecordSource source;

  /**
   * @param source of trust anchors
   */
  public TrustAnchorEntityCustomizer(final CompositeRecordSource source) {
    this.source = source;
  }

  @Override
  public JWTClaimsSet.Builder customize(
      final EntityRecord record,
      final JWTClaimsSet.Builder builder) {

    this.source.getTrustAnchorProperties().stream()
        .filter(ta -> ta.getEntityIdentifier().equals(record.getEntityIdentifier()))
        .findFirst()
        .ifPresent(ta -> {
          Optional.ofNullable(ta.getTrustMarkIssuers()).ifPresent(issuers -> {
            builder.claim("trust_mark_issuers", ta.getTrustMarkIssuers());
          });

          builder.claim("trust_mark_owners", new HashMap<>(ta.trustMarkOwnersJson()));
        });
    return builder;
  }
}
