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
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicyEntry;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperationApplication;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.EssentialOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.OneOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.SubsetOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.SupersetOfOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.ValueOperation;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.common.entity.exception.InvalidMetadataException;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.nimbus.policy.ExtendedMetadataPolicy;
import se.swedenconnect.oidf.nimbus.policy.ExtendedPolicyOperationCombinationValidator;
import se.swedenconnect.oidf.nimbus.policy.ExtendedPolicyOperationFactory;
import se.swedenconnect.oidf.nimbus.policy.ExtendedValueOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

  /** The {@code scope} parameter is a space separated string that operators handle as an array (Section 6.1.3.1.8). */
  private static final String SCOPE = "scope";

  private static final String SCOPE_SEPARATOR = " ";

  private static final int MAX_SCOPE_LENGTH = 8192;

  private static final Set<OperationName> VALUE_CHECKS =
      Set.of(OneOfOperation.NAME, SubsetOfOperation.NAME, SupersetOfOperation.NAME);

  private final PolicyOperationFactory operationFactory = new ExtendedPolicyOperationFactory();

  /**
   * Constructor.
   */
  public MetadataProcessor() {
  }

  /**
   * Resolves the metadata of the trust chain subject (OpenID Federation 1.0, Section 6.1.4).
   *
   * @param chain to process, leaf first
   * @return final metadata object
   * @throws InvalidMetadataException on a metadata policy error
   */
  public JSONObject processMetadata(final List<SignedJWT> chain) throws InvalidMetadataException {
    try {
      final SignedJWT leafNode = chain.getFirst();

      // Entity types not allowed by the allowed_entity_types constraints are removed (Section 6.2.3)
      final Set<String> allowedTypes = allowedEntityTypes(chain);
      // metadata is OPTIONAL in an Entity Configuration
      final Map<String, Object> leafMetadata = Optional.ofNullable(
          EntityStatementClaims.claims(leafNode).getJSONObjectClaim("metadata")).orElseGet(Map::of);
      final List<String> metadataType = leafMetadata
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
        final MetadataPolicy typePolicy = this.resolvePolicy(chain, type);
        final JSONObject baseMetadata = mergeSubordinateMetadata(
            EntityStatementClaims.getMetadata(leafNode, new EntityType(type)),
            immediateSuperiorStatement,
            type);
        result.put(type, applyPolicy(baseMetadata, typePolicy));
      }
      return result;
    }
    catch (final MetadataPolicyException | PolicyViolationException | ParseException
                 | java.text.ParseException e) {
      throw new InvalidMetadataException("Failed to resolve metadata: " + e.getMessage(), e);
    }
  }

  /**
   * Resolves the metadata policy for one entity type by merging the policies of the Subordinate Statements in the
   * chain, starting with the statement issued by the most superior entity (Section 6.1.4.1).
   *
   * @param chain the trust chain, leaf first
   * @param type the entity type
   * @return the resolved policy
   * @throws MetadataPolicyException if the policy is malformed or uses an unsupported critical operator
   * @throws PolicyViolationException if the policies cannot be merged
   * @throws ParseException if an operator value cannot be parsed
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private MetadataPolicy resolvePolicy(final List<SignedJWT> chain, final String type)
      throws MetadataPolicyException, PolicyViolationException, ParseException, java.text.ParseException {
    final List<MetadataPolicy> policies = new ArrayList<>();
    for (final SignedJWT statement : chain.reversed()) {
      if (EntityStatementClaims.isSelfStatement(statement)) {
        // metadata_policy is only used from Subordinate Statements (Sections 3.2 and 6.1.4.1)
        continue;
      }
      final MetadataPolicy statementPolicy = this.parsePolicy(statement, type);
      if (statementPolicy != null) {
        policies.add(statementPolicy);
      }
    }
    if (policies.isEmpty()) {
      return new MetadataPolicy();
    }
    return MetadataPolicy.combine(policies, new ExtendedPolicyOperationCombinationValidator());
  }

  /**
   * Applies a resolved metadata policy to the metadata of one entity type (Section 6.1.4.2).
   *
   * @param metadata the metadata, may be null
   * @param policy the resolved policy
   * @return the resulting metadata, or null if the metadata is null and there is no policy
   * @throws PolicyViolationException if the metadata does not comply with the policy
   */
  private static JSONObject applyPolicy(final JSONObject metadata, final MetadataPolicy policy)
      throws PolicyViolationException {
    if (metadata == null && policy.entrySet().isEmpty()) {
      return null;
    }
    final JSONObject input = metadata == null ? new JSONObject() : new JSONObject(metadata);
    final boolean scopePolicy = policy.getEntry(SCOPE) != null;
    if (scopePolicy && input.get(SCOPE) instanceof final String scope) {
      input.put(SCOPE, splitScope(scope));
    }
    final JSONObject result = apply(policy, input);
    if (scopePolicy && result.get(SCOPE) instanceof final List<?> scopes
        && scopes.stream().allMatch(String.class::isInstance)) {
      result.put(SCOPE, String.join(" ", scopes.stream().map(String.class::cast).toList()));
    }
    return result;
  }

  /**
   * Applies a metadata policy in the same way as {@link MetadataPolicy#apply(JSONObject)}, with support for
   * {@link ExtendedValueOperation}s.
   *
   * @param policy the policy
   * @param metadata the metadata
   * @return the resulting metadata
   * @throws PolicyViolationException if the metadata does not comply with the policy
   */
  private static JSONObject apply(final MetadataPolicy policy, final JSONObject metadata)
      throws PolicyViolationException {
    final JSONObject result = new JSONObject();
    for (final Map.Entry<String, Object> parameter : metadata.entrySet()) {
      if (policy.getEntry(parameter.getKey()) == null) {
        result.put(parameter.getKey(), parameter.getValue());
      }
    }
    for (final MetadataPolicyEntry entry : policy.entrySet()) {
      final Object value = applyEntry(entry, metadata.get(entry.getParameterName()));
      if (value != null) {
        result.put(entry.getParameterName(), value);
      }
    }
    return result;
  }

  /**
   * Applies the operations of one policy entry in the same way as {@link MetadataPolicyEntry#apply(Object)}, with
   * support for {@link ExtendedValueOperation}s.
   *
   * @param entry the policy entry
   * @param value the parameter value, null if absent
   * @return the resulting value, null if the parameter is to be removed or stays absent
   * @throws PolicyViolationException if the value does not comply with the policy
   */
  private static Object applyEntry(final MetadataPolicyEntry entry, final Object value)
      throws PolicyViolationException {
    Object updated = value;
    boolean essential = false;
    for (final Map.Entry<OperationName, PolicyOperation> operation : entry.getOperationsMap().entrySet()) {
      final OperationName name = operation.getKey();
      if (value == null && !essential && VALUE_CHECKS.contains(name)) {
        continue;
      }
      if (operation.getValue() instanceof final ExtendedValueOperation extended) {
        updated = extended.apply(updated);
      }
      else {
        updated = PolicyOperationApplication.apply(operation.getValue(), updated);
      }
      if (ValueOperation.NAME.equals(name)) {
        return updated;
      }
      if (operation.getValue() instanceof final EssentialOperation essentialOperation) {
        if (essentialOperation.getBooleanConfiguration()) {
          essential = true;
        }
        else if (updated == null) {
          return null;
        }
      }
      if (SubsetOfOperation.NAME.equals(name) && essential && updated == null) {
        throw new PolicyViolationException("Essential parameter failed subset_of check");
      }
    }
    return updated;
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
   * @return the policy, or null if the statement has no policy for the type
   * @throws MetadataPolicyException if the policy is malformed or uses an unsupported critical operator
   * @throws PolicyViolationException if the policy has an invalid combination of operators
   * @throws ParseException if an operator value cannot be parsed
   * @throws java.text.ParseException if the statement claims cannot be parsed
   */
  private MetadataPolicy parsePolicy(final SignedJWT statement, final String type)
      throws MetadataPolicyException, PolicyViolationException, ParseException, java.text.ParseException {
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

    final JSONObject parsed = new JSONObject();
    for (final Map.Entry<?, ?> parameter : typePolicy.entrySet()) {
      final String parameterName = String.valueOf(parameter.getKey());
      if (!(parameter.getValue() instanceof final Map<?, ?> operators)) {
        throw new MetadataPolicyException("metadata_policy for '%s.%s' is not a JSON object"
            .formatted(type, parameterName));
      }
      final JSONObject supportedOperators = new JSONObject();
      for (final Map.Entry<?, ?> operator : operators.entrySet()) {
        final String name = String.valueOf(operator.getKey());
        if (this.operationFactory.createForName(new OperationName(name)) != null) {
          if (SCOPE.equals(parameterName) && operator.getValue() instanceof final String scope) {
            supportedOperators.put(name, splitScope(scope));
          }
          else {
            supportedOperators.put(name, operator.getValue());
          }
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
        parsed.put(parameterName, supportedOperators);
      }
    }
    return ExtendedMetadataPolicy.parse(parsed);
  }

  /**
   * Splits a scope string on {@value #SCOPE_SEPARATOR}. A single character separator makes {@link String#split}
   * skip the regex engine.
   *
   * @param scope the scope string
   * @return the scope values
   * @throws PolicyViolationException if the scope string exceeds {@value #MAX_SCOPE_LENGTH} characters
   */
  private static List<String> splitScope(final String scope) throws PolicyViolationException {
    if (scope.length() > MAX_SCOPE_LENGTH) {
      throw new PolicyViolationException("scope exceeds %d characters".formatted(MAX_SCOPE_LENGTH));
    }
    return new ArrayList<>(Arrays.stream(scope.split(SCOPE_SEPARATOR)).filter(s -> !s.isEmpty()).toList());
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
