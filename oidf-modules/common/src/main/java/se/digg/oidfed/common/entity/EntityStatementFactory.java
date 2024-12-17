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
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Factory class for creating entity statements.
 *
 * @author Felix Hellman
 */
public class EntityStatementFactory {
  /**
   * Creates an entity statement that is self-signed
   * @param record to construct entity configuration for
   * @return new instance
   */

  private final JWK signKey;

  /**
   * @param signKey to use
   */
  public EntityStatementFactory(final JWK signKey) {
    this.signKey = signKey;
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
      builder.issuer(record.getSubject().getValue());
      builder.claim("jwks", new JWKSet(List.of(this.signKey)).toJSONObject());
      return EntityStatement.sign(new EntityStatementClaimsSet(builder.build()), this.signKey);
    }
    catch (JOSEException | ParseException e) {
      throw new IllegalArgumentException("Failed to sign entity configuration", e);
    }
  }
}
