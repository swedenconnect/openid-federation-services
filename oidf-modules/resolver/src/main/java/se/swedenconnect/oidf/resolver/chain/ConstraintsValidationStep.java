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
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.EntityIDConstraint;
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.TrustChainConstraints;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates constraints of the chain.
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
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void verifyIndividualConstraint(final TrustChainConstraints constraints,
      final List<EntityStatement> subordinateStatements) throws BadJOSEException, JOSEException {
    if (constraints == null) {
      return;
    }
    if (subordinateStatements.isEmpty()) {
      return;
    }

    // Extract constraints components
    final int maxPathLength = constraints.getMaxPathLength();
    final List<String> allowedLeafEntityTypes = constraints.getLeafEntityTypeConstraint().getAllowedAsStringList();
    final List<String> excluded =
        constraints.getExcludedEntityIDs().stream().map(EntityIDConstraint::toString).toList();
    final List<String> permitted =
        constraints.getPermittedEntityIDs().stream().map(EntityIDConstraint::toString).toList();
    final EntityStatement leafStatement = subordinateStatements.getLast();

    final Map<String, Object> leafEntityMetadataJsonObject = leafStatement.getClaimsSet().getMetadataPolicyJSONObject();
    final List<String> leafEntityTypes = leafEntityMetadataJsonObject.keySet().stream()
        .filter(s -> !"federation_entity".equals(s))
        .filter(
            s -> leafEntityMetadataJsonObject.get(s) != null && !((Map) leafEntityMetadataJsonObject.get(s)).isEmpty())
        .toList();
    final List<String> subjectEntityIdentifiers = subordinateStatements.stream()
        .map(EntityStatement::getClaimsSet)
        .map(CommonClaimsSet::getSubject)
        .map(Identifier::getValue)
        .toList();

    // Check max path length = the number of allowed intermediates
    int intermediateCount = subordinateStatements.size();

    if (leafStatement.getClaimsSet().isSelfStatement() && Objects.nonNull(
        leafStatement.verifySignatureOfSelfStatement())) {
      // This implementation allows a chain to end with an Entity Statement.
      // If the last statement is selfsigned it is not counted as an Intermediate Entity statement
      intermediateCount -= 1;
    }
    if (intermediateCount > maxPathLength) {
      throw new IllegalArgumentException("Max path length constraints check failed");
    }

    // Check naming constraints
    if (excluded != null && !excluded.isEmpty()) {
      // Fail if any subject Entity Identifier starts with any declared excluded name
      if (subjectEntityIdentifiers.stream().anyMatch(subjectId -> excluded.stream().anyMatch(subjectId::startsWith))
      ) {
        throw new IllegalArgumentException("Excluded name constraints violation");
      }
    }
    if (permitted != null && !permitted.isEmpty()) {
      // Fail if not all subject Entity Identifiers starts with at least one of the permitted names
      if (!subjectEntityIdentifiers.stream()
          .allMatch(subjectId -> permitted.stream().anyMatch(subjectId::startsWith))) {
        throw new IllegalArgumentException("Permitted name constraints violation");
      }
    }

    // Check leaf entity types
    if (allowedLeafEntityTypes != null && !allowedLeafEntityTypes.isEmpty()) {
      if (!new HashSet<>(allowedLeafEntityTypes).containsAll(leafEntityTypes)) {
        throw new IllegalArgumentException("Leaf entity type constraints violation");
      }
    }

  }
}
