/*
 * Copyright 2026 Sweden Connect
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
package se.swedenconnect.oidf.nimbus.policy;

import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates the combinations of the standard operators in a metadata parameter policy according to OpenID Federation
 * 1.0, Section 6.1.3.1. The checks are made on the configured JSON values, so they work for both the extended
 * operators and the operators of the Nimbus SDK.
 * <p>
 * It replaces the Nimbus SDK's
 * {@link com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator},
 * which follows an earlier draft of the specification, where for example {@code default} must be a subset of
 * {@code subset_of}.
 * </p>
 * <p>
 * Operators that are not defined by the specification are not checked. The operations are returned in the order they
 * were given; the Nimbus SDK applies them in its own fixed order.
 * </p>
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
public class ExtendedPolicyOperationCombinationValidator implements PolicyOperationCombinationValidator {

  private static final String VALUE = "value";
  private static final String ADD = "add";
  private static final String DEFAULT = "default";
  private static final String ONE_OF = "one_of";
  private static final String SUBSET_OF = "subset_of";
  private static final String SUPERSET_OF = "superset_of";
  private static final String ESSENTIAL = "essential";

  /**
   * Constructor.
   */
  public ExtendedPolicyOperationCombinationValidator() {
  }

  @Override
  public List<PolicyOperation> validate(final List<PolicyOperation> policyOperations)
      throws PolicyViolationException {
    final Map<String, Object> configurations = new HashMap<>();
    for (final PolicyOperation operation : policyOperations) {
      configurations.put(operation.getOperationName().getValue(), operation.toJSONObjectEntry().getValue());
    }
    if (configurations.containsKey(VALUE)) {
      this.validateValueCombinations(configurations);
    }
    if (configurations.containsKey(ONE_OF)) {
      for (final String other : List.of(ADD, SUBSET_OF, SUPERSET_OF)) {
        if (configurations.containsKey(other)) {
          throw this.combinationError("one_of cannot be combined with " + other);
        }
      }
    }
    if (configurations.containsKey(ADD) && configurations.containsKey(SUBSET_OF)
        && !JsonValues.containsAll(JsonValues.asList(configurations.get(SUBSET_OF)),
        JsonValues.asList(configurations.get(ADD)))) {
      throw this.combinationError("add must be a subset of subset_of");
    }
    if (configurations.containsKey(SUBSET_OF) && configurations.containsKey(SUPERSET_OF)
        && !JsonValues.containsAll(JsonValues.asList(configurations.get(SUBSET_OF)),
        JsonValues.asList(configurations.get(SUPERSET_OF)))) {
      throw this.combinationError("subset_of must be a superset of superset_of");
    }
    return new ArrayList<>(policyOperations);
  }

  /**
   * Validates the combinations of {@code value} with the other operators (Section 6.1.3.1.1). When {@code value} is
   * {@code null} the metadata parameter is removed, so its value is not checked against {@code one_of},
   * {@code subset_of} and {@code superset_of}.
   *
   * @param configurations the operator configurations, by operator name
   * @throws PolicyViolationException if the combination is not allowed
   */
  private void validateValueCombinations(final Map<String, Object> configurations) throws PolicyViolationException {
    final Object value = configurations.get(VALUE);
    final List<?> values = JsonValues.asList(value);
    if (configurations.containsKey(ADD)
        && (value == null || !JsonValues.containsAll(values, JsonValues.asList(configurations.get(ADD))))) {
      throw this.combinationError("add must be a subset of value");
    }
    if (configurations.containsKey(DEFAULT) && value == null) {
      throw this.combinationError("default cannot be combined with a null value");
    }
    if (value == null && Boolean.TRUE.equals(configurations.get(ESSENTIAL))) {
      throw this.combinationError("a null value cannot be combined with essential true");
    }
    if (value == null) {
      return;
    }
    if (configurations.containsKey(ONE_OF)
        && !JsonValues.contains(JsonValues.asList(configurations.get(ONE_OF)), value)) {
      throw this.combinationError("value must be among the one_of values");
    }
    if (configurations.containsKey(SUBSET_OF)
        && !JsonValues.containsAll(JsonValues.asList(configurations.get(SUBSET_OF)), values)) {
      throw this.combinationError("value must be a subset of subset_of");
    }
    if (configurations.containsKey(SUPERSET_OF)
        && !JsonValues.containsAll(values, JsonValues.asList(configurations.get(SUPERSET_OF)))) {
      throw this.combinationError("value must be a superset of superset_of");
    }
  }

  private PolicyViolationException combinationError(final String message) {
    return new PolicyViolationException("Invalid policy operation combination: " + message);
  }
}
