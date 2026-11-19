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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates constraints of the chain.
 * Supports the following constraint parameters
 * <ul>
 * <li>max_path_length</li>
 * <li>naming_constraints</li>
 * </ul>
 * The allowed_entity_types constraint does not invalidate the chain. Entity types that are not allowed are removed
 * from the metadata by {@link se.swedenconnect.oidf.resolver.metadata.MetadataProcessor}.
 *
 * @author Felix Hellman
 */
public class ConstraintsValidationStep implements ChainValidationStep {
  @Override
  public List<ChainValidationError> validate(final List<SignedJWT> chain) {
    final ArrayList<ChainValidationError> errors = new ArrayList<>();
    for (int x = chain.size() - 1; x >= 0; x--) {
      final SignedJWT current = chain.get(x);
      //TODO check parent is self statement
      try {
        final Map<String, Object> jsonConstraints =
            EntityStatementClaims.claims(current).getJSONObjectClaim("constraints");
        if (Objects.nonNull(jsonConstraints)) {
          final ConstraintRecord constraints = ConstraintRecord.fromJson(jsonConstraints);
          errors.addAll(this.verifySubordinates(constraints, chain.subList(0, x)));
        }
      } catch (final Exception e) {
        errors.add(new ChainValidationError(ChainValidationErrorType.MALFORMED_STATEMENT,
            "Failed to validate constraints of statement at position %d: %s".formatted(x, e.getMessage()), e));
      }
    }
    return errors;
  }

  private List<ChainValidationError> verifySubordinates(
      final ConstraintRecord constraints,
      final List<SignedJWT> subordinateStatements)
      throws BadJOSEException, JOSEException, java.text.ParseException {

    if (subordinateStatements.isEmpty()) {
      return List.of();
    }
    final List<ChainValidationError> errors = new ArrayList<>();

    final SignedJWT leafStatement = subordinateStatements.getFirst();

    // Check max path length = the number of allowed intermediates
    int intermediateCount = subordinateStatements.size();

    if (EntityStatementClaims.isSelfStatement(leafStatement) && Objects.nonNull(
        EntityStatementClaims.verifySignatureOfSelfStatement(leafStatement))) {
      // This implementation allows a chain to end with an Entity Statement.
      // If the last statement is selfsigned it is not counted as an Intermediate Entity statement
      intermediateCount -= 1;
    }

    if (Objects.nonNull(constraints.getMaxPathLength())) {
      if (intermediateCount > constraints.getMaxPathLength()) {
        errors.add(new ChainValidationError(ChainValidationErrorType.MAX_PATH_LENGTH_EXCEEDED,
            "Max path length constraints check failed: %d intermediates, max_path_length is %d"
                .formatted(intermediateCount, constraints.getMaxPathLength()), null));
      }
    }


    final List<String> subjectEntityIdentifiers = subordinateStatements.stream()
        .map(es -> EntityStatementClaims.claims(es).getSubject())
        .toList();

    if (Objects.nonNull(constraints.getNaming())) {
      errors.addAll(checkNamingConstraints(constraints, subjectEntityIdentifiers));
    }
    return errors;
  }

  private static List<ChainValidationError> checkNamingConstraints(
      final ConstraintRecord constraints,
      final List<String> subjectEntityIdentifiers) {

    final List<ChainValidationError> errors = new ArrayList<>();
    final List<String> excluded =
        constraints.getNaming().getExcluded()
            .stream()
            .toList();
    final List<String> permitted =
        constraints.getNaming().getPermitted()
            .stream()
            .toList();
    if (!excluded.isEmpty()) {
      //NONE of the subjects may match ANY rule
      final List<String> notAllowed = subjectEntityIdentifiers.stream()
          .filter(subjectId -> EntityNameValidator.anyMatch(subjectId, excluded))
          .toList();
      if (!notAllowed.isEmpty()) {
        errors.add(new ChainValidationError(ChainValidationErrorType.NAMING_CONSTRAINT_EXCLUDED_VIOLATION,
            "Excluded name constraints violation: %s".formatted(notAllowed), null));
      }
    }
    if (!permitted.isEmpty()) {
      //ALL subjects needs to match at least ONE permitted rule
      final List<String> notPermitted = subjectEntityIdentifiers.stream()
          .filter(subjectId -> !EntityNameValidator.anyMatch(subjectId, permitted))
          .toList();
      if (!notPermitted.isEmpty()) {
        errors.add(new ChainValidationError(ChainValidationErrorType.NAMING_CONSTRAINT_PERMITTED_VIOLATION,
            "Permitted name constraints violation: %s".formatted(notPermitted), null));
      }
    }
    return errors;
  }
}
