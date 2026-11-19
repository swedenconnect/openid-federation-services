/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jwt.SignedJWT;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates crit and metadata_policy_crit claims of the chain.
 *
 * @author Felix Hellman
 */
public class CriticalClaimsValidationStep implements ChainValidationStep {

  /**
   * This implementation supports the ec_location claim. Claims defined by OpenID Federation 1.0 must not be listed
   * in crit, so they are rejected as well.
   */
  public static final Set<String> SUPPORTED_CRITICAL_CLAIMS =
      Set.of("ec_location");

  /**
   * This implementation supports no additional metadata operators. Operators defined by OpenID Federation 1.0 must
   * not be listed in metadata_policy_crit, so they are rejected as well.
   */
  public static final Set<String> SUPPORTED_METADATA_CLAIMS = Set.of();

  @Override
  public List<ChainValidationError> validate(final List<SignedJWT> chain) {
    final ArrayList<ChainValidationError> errors = new ArrayList<>();
    chain.forEach(es -> errors.addAll(validateStatement(es)));
    return errors;
  }

  private static List<ChainValidationError> validateStatement(final SignedJWT es) {
    final List<ChainValidationError> errors = new ArrayList<>();
    final String statement = describe(es);
    Optional.ofNullable(EntityStatementClaims.getCriticalExtensionClaims(es))
        .filter(crit -> !SUPPORTED_CRITICAL_CLAIMS.containsAll(crit))
        .ifPresent(crit -> errors.add(error("Unsupported claims in crit of Entity Statement %s: %s"
            .formatted(statement, unsupported(crit, SUPPORTED_CRITICAL_CLAIMS)))));
    final List<String> metadataPolicyCrit;
    try {
      metadataPolicyCrit = EntityStatementClaims.claims(es).getStringListClaim("metadata_policy_crit");
    } catch (final java.text.ParseException e) {
      errors.add(new ChainValidationError(ChainValidationErrorType.MALFORMED_STATEMENT,
          "Failed to parse metadata_policy_crit claim of Entity Statement %s".formatted(statement), e));
      return errors;
    }
    if (metadataPolicyCrit != null && EntityStatementClaims.isSelfStatement(es)) {
      errors.add(error("metadata_policy_crit is only allowed in Subordinate Statements, found in %s"
          .formatted(statement)));
    }
    Optional.ofNullable(metadataPolicyCrit)
        .filter(critMetadata -> critMetadata.isEmpty() || !SUPPORTED_METADATA_CLAIMS.containsAll(critMetadata))
        .ifPresent(critMetadata -> errors.add(error(
            "Unsupported operators in metadata_policy_crit of Entity Statement %s: %s".formatted(statement,
                critMetadata.isEmpty() ? "empty array" : unsupported(critMetadata, SUPPORTED_METADATA_CLAIMS)))));
    return errors;
  }

  private static ChainValidationError error(final String message) {
    return new ChainValidationError(ChainValidationErrorType.UNSUPPORTED_CRITICAL_CLAIMS, message, null);
  }

  private static String describe(final SignedJWT es) {
    try {
      return "issued by %s about %s".formatted(es.getJWTClaimsSet().getIssuer(), es.getJWTClaimsSet().getSubject());
    } catch (final java.text.ParseException e) {
      return "with unparsable claims";
    }
  }

  private static List<String> unsupported(final List<String> values, final Set<String> supported) {
    return values.stream().filter(value -> !supported.contains(value)).toList();
  }
}
