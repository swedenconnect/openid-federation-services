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
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Combines metadata policy and calculates what the metadata value should be.
 *
 * @author Felix Hellman
 */
@Slf4j
public class MetadataProcessor {

  private static final String FEDERATION_ENTITY = "federation_entity";

  /**
   * Constructor.
   */
  public MetadataProcessor() {
  }

  /**
   * @param chain to process
   * @return final metadata object
   */
  public JSONObject processMetadata(final List<SignedJWT> chain) {
    try {
      final SignedJWT leafNode = chain.getFirst();

      // Entity types not allowed by the allowed_entity_types constraints are removed (Section 6.2.3)
      final Set<String> allowedTypes = allowedEntityTypes(chain);
      final List<String> metadataType = EntityStatementClaims.claims(leafNode)
          .getJSONObjectClaim("metadata")
          .keySet()
          .stream()
          .filter(type -> allowedTypes == null || FEDERATION_ENTITY.equals(type) || allowedTypes.contains(type))
          .toList();

      // chain.get(1), if present, is the immediate superior's subordinate statement about the leaf -
      // the only statement whose "metadata" claim is in scope for the leaf, per spec ("Immediate Subordinate").
      final SignedJWT immediateSuperiorStatement = chain.size() > 1 ? chain.get(1) : null;

      // Policies are bound to an entity type and are merged and applied per type (Sections 6.1.1 and 6.1.4)
      final JSONObject result = new JSONObject();
      for (final String type : metadataType) {
        final Map<String, ParameterPolicy> typePolicy = this.resolvePolicy(chain, type);
        final JSONObject baseMetadata = mergeSubordinateMetadata(
            EntityStatementClaims.getMetadata(leafNode, new EntityType(type)),
            immediateSuperiorStatement,
            type);
        result.put(type, applyPolicy(baseMetadata, typePolicy));
      }
      return result;
    }
    catch (final MetadataPolicyException | java.text.ParseException e) {
      throw new IllegalArgumentException("Failed to validate/parse policy", e);
    }
  }

  /**
   * Resolves the metadata policy for one entity type by merging the policies of the statements in the chain, starting
   * with the statement issued by the most superior entity (Section 6.1.4.1).
   *
   * @param chain the trust chain, leaf first
   * @param type the entity type
   * @return the resolved policy, keyed by metadata parameter
   * @throws MetadataPolicyException on a policy error
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private Map<String, ParameterPolicy> resolvePolicy(final List<SignedJWT> chain, final String type)
      throws MetadataPolicyException, java.text.ParseException {
    final Map<String, ParameterPolicy> resolved = new LinkedHashMap<>();
    for (final SignedJWT statement : chain.reversed()) {
      final Map<String, ParameterPolicy> statementPolicy = this.parsePolicy(statement, type);
      if (statementPolicy == null) {
        continue;
      }
      for (final Map.Entry<String, ParameterPolicy> parameter : statementPolicy.entrySet()) {
        final ParameterPolicy current = resolved.get(parameter.getKey());
        resolved.put(parameter.getKey(), current == null ? parameter.getValue() : current.merge(parameter.getValue()));
      }
    }
    return resolved;
  }

  /**
   * Applies a resolved metadata policy to the metadata of one entity type (Section 6.1.4.2).
   *
   * @param metadata the metadata, may be null
   * @param policy the resolved policy
   * @return the resulting metadata, or null if the metadata is null and there is no policy
   * @throws MetadataPolicyException if the metadata does not comply with the policy
   */
  private static JSONObject applyPolicy(final JSONObject metadata, final Map<String, ParameterPolicy> policy)
      throws MetadataPolicyException {
    if (metadata == null && policy.isEmpty()) {
      return null;
    }
    final JSONObject result = metadata == null ? new JSONObject() : new JSONObject(metadata);
    for (final Map.Entry<String, ParameterPolicy> parameter : policy.entrySet()) {
      final Object value = parameter.getValue().apply(result.get(parameter.getKey()));
      if (value == null) {
        result.remove(parameter.getKey());
      }
      else {
        result.put(parameter.getKey(), value);
      }
    }
    return result;
  }

  /**
   * Collects the {@code allowed_entity_types} constraints of the Subordinate Statements in the chain. When several
   * statements set the constraint, only types allowed by all of them are allowed.
   *
   * @param chain the trust chain
   * @return the allowed entity types, or null if no statement sets the constraint
   * @throws MetadataPolicyException if a constraint is not an array of strings
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private static Set<String> allowedEntityTypes(final List<SignedJWT> chain)
      throws MetadataPolicyException, java.text.ParseException {
    Set<String> allowed = null;
    for (final SignedJWT statement : chain) {
      if (EntityStatementClaims.isSelfStatement(statement)) {
        continue;
      }
      final Map<String, Object> constraints = EntityStatementClaims.claims(statement).getJSONObjectClaim("constraints");
      if (constraints == null || !constraints.containsKey("allowed_entity_types")) {
        continue;
      }
      if (!(constraints.get("allowed_entity_types") instanceof final List<?> types)
          || !types.stream().allMatch(String.class::isInstance)) {
        throw new MetadataPolicyException("allowed_entity_types is not an array of strings");
      }
      final Set<String> statementTypes = types.stream().map(String.class::cast).collect(Collectors.toSet());
      if (allowed == null) {
        allowed = new HashSet<>(statementTypes);
      }
      else {
        allowed.retainAll(statementTypes);
      }
    }
    return allowed;
  }

  /**
   * Parses the metadata policy of one entity type in a statement. Operators that are not supported are ignored,
   * unless they are listed in {@code metadata_policy_crit} (OpenID Federation 1.0, Section 6.1.3.2).
   *
   * @param statement the statement holding the policy
   * @param type the entity type
   * @return the policy keyed by metadata parameter, or null if the statement has no policy for the type
   * @throws MetadataPolicyException if the policy is malformed, has an invalid combination of operators, or uses an
   *     unsupported critical operator
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private Map<String, ParameterPolicy> parsePolicy(final SignedJWT statement, final String type)
      throws MetadataPolicyException, java.text.ParseException {
    final JWTClaimsSet claims = EntityStatementClaims.claims(statement);
    final Map<String, Object> policy = claims.getJSONObjectClaim("metadata_policy");
    if (policy == null || !policy.containsKey(type)) {
      return null;
    }
    if (!(policy.get(type) instanceof final Map<?, ?> typePolicy)) {
      throw new MetadataPolicyException("metadata_policy for '%s' is not a JSON object".formatted(type));
    }
    final List<String> critical =
        Optional.ofNullable(claims.getStringListClaim("metadata_policy_crit")).orElseGet(List::of);

    final Map<String, ParameterPolicy> parsed = new LinkedHashMap<>();
    for (final Map.Entry<?, ?> parameter : typePolicy.entrySet()) {
      final String parameterName = String.valueOf(parameter.getKey());
      if (!(parameter.getValue() instanceof final Map<?, ?> operators)) {
        throw new MetadataPolicyException("metadata_policy for '%s.%s' is not a JSON object"
            .formatted(type, parameterName));
      }
      final Map<String, Object> supportedOperators = new LinkedHashMap<>();
      for (final Map.Entry<?, ?> operator : operators.entrySet()) {
        final String name = String.valueOf(operator.getKey());
        if (ParameterPolicy.isSupported(name)) {
          supportedOperators.put(name, operator.getValue());
        }
        else if (critical.contains(name)) {
          throw new MetadataPolicyException("Critical policy operator '%s' is not supported".formatted(name));
        }
        else {
          log.debug("Ignoring unsupported policy operator '{}' for '{}.{}' in statement from '{}'",
              name, type, parameterName, claims.getIssuer());
        }
      }
      if (!supportedOperators.isEmpty()) {
        parsed.put(parameterName, ParameterPolicy.parse(parameterName, supportedOperators));
      }
    }
    return parsed;
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
