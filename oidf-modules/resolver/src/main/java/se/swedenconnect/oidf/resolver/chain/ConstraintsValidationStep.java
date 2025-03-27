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
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.EntityIDConstraint;
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.TrustChainConstraints;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    for (int i = 1; i < chain.size(); i++) {
      try {
        this.verifyIndividualConstraint(chain.get(i - 1).getClaimsSet().getConstraints(), chain.subList(i,
            chain.size()));
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void verifyIndividualConstraint(
      final TrustChainConstraints constraints,
      final List<EntityStatement> subordinateStatements) throws BadJOSEException, JOSEException {

    if (constraints == null) {
      return;
    }
    if (subordinateStatements.isEmpty()) {
      return;
    }

    final EntityStatement leafStatement = subordinateStatements.getLast();

    //If allowedLeafEntityTypes is empty we implicitly allow all types
    final Set<String> allowedLeafEntityTypes = Optional.ofNullable(constraints.getLeafEntityTypeConstraint()
            .getAllowed()).map(allowed -> allowed.stream().map(Identifier::getValue).collect(Collectors.toSet()))
        .orElse(Set.of());
    if (!allowedLeafEntityTypes.isEmpty()) {
      final EntityStatementClaimsSet claimsSet = leafStatement.getClaimsSet();
      final HashMap<String, Object> tmpMetadata = new HashMap<>(claimsSet.getJSONObjectClaim("metadata"));
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

    // Check max path length = the number of allowed intermediates
    int intermediateCount = subordinateStatements.size();

    if (leafStatement.getClaimsSet().isSelfStatement() && Objects.nonNull(
        leafStatement.verifySignatureOfSelfStatement())) {
      // This implementation allows a chain to end with an Entity Statement.
      // If the last statement is selfsigned it is not counted as an Intermediate Entity statement
      intermediateCount -= 1;
    }

    if (intermediateCount > constraints.getMaxPathLength()) {
      throw new IllegalArgumentException("Max path length constraints check failed");
    }


    final List<String> subjectEntityIdentifiers = subordinateStatements.stream()
        .map(EntityStatement::getClaimsSet)
        .map(CommonClaimsSet::getSubject)
        .map(Identifier::getValue)
        .toList();

    checkNamingConstraints(constraints, subjectEntityIdentifiers);
  }

  private static void checkNamingConstraints(
      final TrustChainConstraints constraints,
      final List<String> subjectEntityIdentifiers) {

    final List<String> excluded =
        constraints.getExcludedEntityIDs()
            .stream()
            .map(EntityIDConstraint::toString)
            .toList();
    final List<String> permitted =
        constraints.getPermittedEntityIDs()
            .stream()
            .map(EntityIDConstraint::toString)
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
