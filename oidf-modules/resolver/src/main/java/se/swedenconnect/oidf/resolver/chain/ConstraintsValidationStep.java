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
package se.swedenconnect.oidf.resolver.chain;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.claims.CommonClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ConstraintRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates constraints of the chain.
 * Supports the following constraint parameters
 * <ul>
 * <li>max_path_length</li>
 * <li>naming_constraints</li>
 * <li>allowed_entity_types</li>
 * </ul>
 *
 * @author Felix Hellman
 */
public class ConstraintsValidationStep implements ChainValidationStep {
  @Override
  public void validate(final List<EntityStatement> chain) {
    try {
      for (int x = chain.size() - 1; x >= 0; x--) {
        final EntityStatement current = chain.get(x);
        //TODO check parent is self statement
        final JSONObject jsonConstraints = current.getClaimsSet().getJSONObjectClaim("constraints");
        if (Objects.nonNull(jsonConstraints)) {
          final ConstraintRecord constraints = ConstraintRecord.fromJson(jsonConstraints);
          this.verifySubordinates(constraints, chain.subList(0, x));
        }

      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to validate", e);
    }
  }

  private void verifySubordinates(
      final ConstraintRecord constraints,
      final List<EntityStatement> subordinateStatements) throws BadJOSEException, JOSEException {

    if (subordinateStatements.isEmpty()) {
      return;
    }

    final EntityStatement leafStatement = subordinateStatements.getFirst();

    //If allowedLeafEntityTypes is empty we implicitly allow all types
    final Set<String> allowedLeafEntityTypes = Optional.ofNullable(constraints.getAllowedEntityTypes())
        .map(HashSet::new)
        .orElse(new HashSet<>());
    if (!allowedLeafEntityTypes.isEmpty()) {
      final EntityStatementClaimsSet claimsSet = leafStatement.getClaimsSet();
      final JSONObject metadataClaim = claimsSet.getJSONObjectClaim("metadata");
      if (Objects.nonNull(metadataClaim)) {
        final HashMap<String, Object> tmpMetadata = new HashMap<>(metadataClaim);
        //This implementation allows federation_entity implicitly
        tmpMetadata.remove("federation_entity");
        final Set<String> keys = tmpMetadata.keySet();
        //Remove all permitted entity_types
        keys.removeAll(allowedLeafEntityTypes);
        //If keys is not empty by now, it contains one or more illegal entity types.
        if (!keys.isEmpty()) {
          throw new IllegalArgumentException(
              "Leaf entity entity_type constraints violation illegal-types:%s".formatted(keys)
          );
        }
      }
    }

    // Check max path length = the number of allowed intermediates
    int intermediateCount = subordinateStatements.size();

    if (leafStatement.getClaimsSet().isSelfStatement() && Objects.nonNull(
        leafStatement.verifySignatureOfSelfStatement())) {
      // This implementation allows a chain to end with an Entity Statement.
      // If the last statement is selfsigned it is not counted as an Intermediate Entity statement
      intermediateCount -= 1;
    }

    if (Objects.nonNull(constraints.getMaxPathLength())) {
      if (intermediateCount > constraints.getMaxPathLength()) {
        throw new IllegalArgumentException("Max path length constraints check failed");
      }
    }


    final List<String> subjectEntityIdentifiers = subordinateStatements.stream()
        .map(EntityStatement::getClaimsSet)
        .map(CommonClaimsSet::getSubject)
        .map(Identifier::getValue)
        .toList();

    if (Objects.nonNull(constraints.getNamingConstraints())) {
      checkNamingConstraints(constraints, subjectEntityIdentifiers);
    }
  }

  private static void checkNamingConstraints(
      final ConstraintRecord constraints,
      final List<String> subjectEntityIdentifiers) {

    final List<String> excluded =
        constraints.getNamingConstraints().getExcluded()
            .stream()
            .toList();
    final List<String> permitted =
        constraints.getNamingConstraints().getPermitted()
            .stream()
            .toList();
    if (!excluded.isEmpty()) {
      final boolean entityNameIsNotAllowed = subjectEntityIdentifiers.stream()
          //NONE of the subjects may match ANY rule
          .anyMatch(subjectId -> EntityNameValidator.anyMatch(subjectId, excluded));
      if (entityNameIsNotAllowed) {
        throw new IllegalArgumentException("Excluded name constraints violation");
      }
    }
    if (!permitted.isEmpty()) {
      final boolean entityNameIsAllowed = subjectEntityIdentifiers.stream()
          //ALL subjects needs to match at least ONE permitted rule
          .allMatch(subjectId -> EntityNameValidator.anyMatch(subjectId, permitted));
      if (!entityNameIsAllowed) {
        throw new IllegalArgumentException("Permitted name constraints violation");
      }
    }
  }
}
