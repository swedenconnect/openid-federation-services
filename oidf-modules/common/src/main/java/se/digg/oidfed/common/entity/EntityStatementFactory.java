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
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Factory class for creating entity statements.
 *
 * @author Felix Hellman
 */
public class EntityStatementFactory {

  private final Map<String, List<Consumer<JWTClaimsSet.Builder>>> customizers = new HashMap<>();

  /**
   * Creates an entity statement that is self-signed
   * @param properties to create from
   * @return new instance
   */
  public EntityStatement createEntityConfiguration(final EntityProperties properties) {
    try {
      final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

      builder.issuer(properties.getEntityIdentifier());
      builder.subject(properties.getEntityIdentifier());
      builder.issueTime(Date.from(Instant.now()));
      builder.expirationTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
      builder.claim("jwks", Map.of("keys", List.of(properties.getSignKey().toJSONObject())));
      final Map<String, Map<String, String>> federationEntity =
          Map.of("federation_entity", Map.of("organization_name", properties.getOrganizationName()));
      builder.claim("metadata", federationEntity);

      Optional.ofNullable(properties.getAuthortyHints()).ifPresent(ah -> builder.claim("authority_hint", ah));

      Optional.ofNullable(customizers.get(properties.getEntityIdentifier()))
          .ifPresent(c -> c.forEach(customizer -> customizer.accept(builder)));

      final JWTClaimsSet jwtClaimsSet = builder.build();

      return EntityStatement.sign(new EntityStatementClaimsSet(jwtClaimsSet), properties.getSignKey());
    }
    catch (JOSEException | ParseException e) {
      throw new IllegalArgumentException("Failed to sign entity configuration", e);
    }
  }
}
