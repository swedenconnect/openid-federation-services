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
package se.swedenconnect.oidf.common.entity.tree;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.text.html.parser.Entity;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wrapper for entity statement JWT.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class EntityStatementWrapper {

  @Getter
  private final SignedJWT entityStatement;

  /**
   * Parses and returns the entity statement from the underlying signed JWT.
   *
   * @return parsed entity statement
   */
  public EntityStatement getEntityStatement() {
    try {
      return EntityStatement.parse(this.entityStatement);
    } catch (final com.nimbusds.oauth2.sdk.ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns trust marks from the underlying entity statement.
   *
   * @return list of trust mark JWTs, empty if none present
   */
  public List<SignedJWT> getTrustMarks() {
    try {
      final List<Object> trustMarks = this.entityStatement.getJWTClaimsSet().getListClaim("trust_marks");
      if (trustMarks == null) {
        return Collections.emptyList();
      }
      return trustMarks.stream()
          .map(e -> (Map<String, Object>) e)
          .map(entry -> {
            try {
              return SignedJWT.parse((String) entry.get("trust_mark"));
            } catch (final ParseException e) {
              throw new IllegalArgumentException("Failed to parse trust mark JWT", e);
            }
          })
          .toList();
    } catch (final ParseException e) {
      throw new IllegalStateException("Failed to parse entity statement claims", e);
    }
  }

  /**
   * Returns the raw federation_entity metadata from the entity statement, if present.
   *
   * @return the federation_entity metadata map, or empty if not present
   */
  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getFederationEntityMetadata() {
    try {
      return Optional.ofNullable(this.entityStatement.getJWTClaimsSet().getJSONObjectClaim("metadata"))
          .flatMap(m -> Optional.ofNullable((Map<String, Object>) m.get("federation_entity")));
    } catch (final ParseException e) {
      throw new IllegalStateException("Failed to parse entity statement claims", e);
    }
  }
}
