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
package se.swedenconnect.oidf.resolver.metadata;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Combines metadata policy and calculates what the metadata value should be.
 *
 * @author Felix Hellman
 */
@Slf4j
public class MetadataProcessor {

  private final PolicyOperationFactory operationFactory;
  private final PolicyOperationCombinationValidator combinationValidator;

  /**
   * Constructor
   * @param operationFactory for creating operations
   * @param combinationValidator for validating operation combinations
   */
  public MetadataProcessor(final PolicyOperationFactory operationFactory,
      final PolicyOperationCombinationValidator combinationValidator) {
    this.operationFactory = operationFactory;
    this.combinationValidator = combinationValidator;
  }

  /**
   * @param chain to process
   * @return final metadata object
   */
  public JSONObject processMetadata(final List<SignedJWT> chain) {
    try {
      final SignedJWT leafNode = chain.getFirst();

      final List<String> metadataType = EntityStatementClaims.claims(leafNode)
          .getJSONObjectClaim("metadata")
          .keySet()
          .stream()
          .toList();

      final List<MetadataPolicy> metadataPolicies = new ArrayList<>();
      for (final SignedJWT statement : chain) {
        for (final String type : metadataType) {
          Optional.ofNullable(this.parsePolicy(statement, type)).ifPresent(metadataPolicies::add);
        }
      }

      final MetadataPolicy combinedMetadataPolicy = MetadataPolicy.combine(metadataPolicies, this.combinationValidator);

      final MetadataPolicy metadataPolicy =
          MetadataPolicy.parse(combinedMetadataPolicy.toJSONObject(), this.operationFactory, this.combinationValidator);

      // chain.get(1), if present, is the immediate superior's subordinate statement about the leaf -
      // the only statement whose "metadata" claim is in scope for the leaf, per spec ("Immediate Subordinate").
      final SignedJWT immediateSuperiorStatement = chain.size() > 1 ? chain.get(1) : null;

      final JSONObject result = new JSONObject();
      metadataType.forEach(type -> {
        try {
          final JSONObject baseMetadata = mergeSubordinateMetadata(
              EntityStatementClaims.getMetadata(leafNode, new EntityType(type)),
              immediateSuperiorStatement,
              type);
          result.put(type, metadataPolicy.apply(baseMetadata));
        }
        catch (final PolicyViolationException e) {
          throw new RuntimeException(e);
        }
      });
      return result;
    }
    catch (final PolicyViolationException | com.nimbusds.oauth2.sdk.ParseException | java.text.ParseException e) {
      throw new IllegalArgumentException("Failed to validate/parse policy", e);
    }
  }

  /**
   * Parses the metadata policy of one entity type in a statement. Operators that are not supported are ignored,
   * unless they are listed in {@code metadata_policy_crit} (OpenID Federation 1.0, Section 6.1.3.2).
   *
   * @param statement the statement holding the policy
   * @param type the entity type
   * @return the policy, or null if the statement has no policy for the type
   * @throws PolicyViolationException if the policy is malformed, has an invalid combination of operators, or uses an
   *     unsupported critical operator
   * @throws com.nimbusds.oauth2.sdk.ParseException if an operator value cannot be parsed
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private MetadataPolicy parsePolicy(final SignedJWT statement, final String type)
      throws PolicyViolationException, com.nimbusds.oauth2.sdk.ParseException, java.text.ParseException {
    final JWTClaimsSet claims = EntityStatementClaims.claims(statement);
    final Map<String, Object> policy = claims.getJSONObjectClaim("metadata_policy");
    if (policy == null || !policy.containsKey(type)) {
      return null;
    }
    if (!(policy.get(type) instanceof final Map<?, ?> typePolicy)) {
      throw new PolicyViolationException("metadata_policy for '%s' is not a JSON object".formatted(type));
    }
    final List<String> critical =
        Optional.ofNullable(claims.getStringListClaim("metadata_policy_crit")).orElseGet(List::of);

    final JSONObject supported = new JSONObject();
    for (final Map.Entry<?, ?> parameter : typePolicy.entrySet()) {
      if (!(parameter.getValue() instanceof final Map<?, ?> operators)) {
        throw new PolicyViolationException("metadata_policy for '%s.%s' is not a JSON object"
            .formatted(type, parameter.getKey()));
      }
      final JSONObject supportedOperators = new JSONObject();
      for (final Map.Entry<?, ?> operator : operators.entrySet()) {
        final String name = String.valueOf(operator.getKey());
        if (this.operationFactory.createForName(new OperationName(name)) != null) {
          supportedOperators.put(name, operator.getValue());
        }
        else if (critical.contains(name)) {
          throw new PolicyViolationException("Critical policy operator '%s' is not supported".formatted(name));
        }
        else {
          log.debug("Ignoring unsupported policy operator '{}' for '{}.{}' in statement from '{}'",
              name, type, parameter.getKey(), claims.getIssuer());
        }
      }
      if (!supportedOperators.isEmpty()) {
        supported.put(String.valueOf(parameter.getKey()), supportedOperators);
      }
    }
    return MetadataPolicy.parse(supported, this.operationFactory, this.combinationValidator);
  }

  private static JSONObject mergeSubordinateMetadata(
      final JSONObject leafMetadata, final SignedJWT immediateSuperiorStatement, final String type) {
    if (immediateSuperiorStatement == null) {
      return leafMetadata;
    }
    final JSONObject superiorMetadata =
        EntityStatementClaims.getMetadata(immediateSuperiorStatement, new EntityType(type));
    if (superiorMetadata == null) {
      return leafMetadata;
    }
    final JSONObject merged = new JSONObject();
    if (leafMetadata != null) {
      merged.putAll(leafMetadata);
    }
    merged.putAll(superiorMetadata);
    return merged;
  }
}
