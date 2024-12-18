/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.common.entity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import net.minidev.json.JSONObject;
import se.digg.oidfed.common.entity.integration.TrustMarkLoadingCache;
import se.digg.oidfed.common.entity.integration.TrustMarkRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory class for creating entity statements.
 *
 * @author Felix Hellman
 */
public class EntityConfigurationFactory {
  private final JWK signKey;

  private final TrustMarkLoadingCache trustMarks;

  /**
   * @param signKey to sign entity statements with
   * @param trustMarks to supply eventual trust marks
   */
  public EntityConfigurationFactory(final JWK signKey, final TrustMarkLoadingCache trustMarks) {
    this.signKey = signKey;
    this.trustMarks = trustMarks;
  }

  /**
   * @param record to create subject from
   * @return entity statement
   */
  public EntityStatement createEntityConfiguration(final EntityRecord record) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
      builder.issuer(record.getIssuer().getValue());
      builder.subject(record.getSubject().getValue());
      builder.issueTime(Date.from(Instant.now()));
      builder.expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
      builder.claim("metadata", record.getHostedRecord().getMetadata());
      builder.claim("authority_hint", record.getIssuer());
      if (Objects.isNull(record.getHostedRecord())) {
        builder.claim("jwks", record.getJwks().toJSONObject());
        final JWK signKey = record.getSignKey();
        return EntityStatement.sign(new EntityStatementClaimsSet(builder.build()), signKey);
      }
      final List<TrustMarkSource> trustMarkSources = record.getHostedRecord().getTrustMarkSources();
      if (Objects.nonNull(trustMarkSources)) {
        final List<JSONObject> trustMarks = trustMarkSources.stream().map(s -> new TrustMarkRequest(record.getSubject(),
                s.getIssuer(),
                new EntityID(s.getTrustMarkId())))
            .map(this.trustMarks::getTrustMark)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(TrustMarkEntry::toJSONObject)
            .toList();
        builder.claim("trust_marks", trustMarks);
      }
      builder.issuer(record.getSubject().getValue());
      builder.claim("jwks", new JWKSet(List.of(this.signKey)).toJSONObject());
      return EntityStatement.sign(new EntityStatementClaimsSet(builder.build()), this.signKey);
    }
    catch (JOSEException | ParseException e) {
      throw new IllegalArgumentException("Failed to sign entity configuration", e);
    }
  }
}
