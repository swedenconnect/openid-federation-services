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
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Combination validator for policies with {@link ExtendedValueOperation}s (OpenID Federation 1.0, Section 6.1.3.1).
 * Combinations of the other operations are checked by a delegate. The validated operations are returned in the order
 * they are applied (Section 6.1.3.2).
 *
 * @author Felix Hellman
 */
public class ExtendedPolicyOperationCombinationValidator implements PolicyOperationCombinationValidator {

  private static final String VALUE = "value";
  private static final String ADD = "add";
  private static final String DEFAULT = "default";
  private static final String ONE_OF = "one_of";
  private static final String SUBSET_OF = "subset_of";
  private static final String SUPERSET_OF = "superset_of";
  private static final String ESSENTIAL = "essential";

  private static final List<String> ORDER = List.of(VALUE, ADD, DEFAULT, ONE_OF, SUBSET_OF, SUPERSET_OF, ESSENTIAL);

  private final PolicyOperationCombinationValidator delegate;

  /**
   * Constructor using {@link DefaultPolicyOperationCombinationValidator} as delegate.
   */
  public ExtendedPolicyOperationCombinationValidator() {
    this(new DefaultPolicyOperationCombinationValidator());
  }

  /**
   * Constructor.
   *
   * @param delegate validates the combinations of the operations that are not extended
   */
  public ExtendedPolicyOperationCombinationValidator(final PolicyOperationCombinationValidator delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<PolicyOperation> validate(final List<PolicyOperation> policyOperations)
      throws PolicyViolationException {
    final List<PolicyOperation> others = new ArrayList<>(policyOperations.stream()
        .filter(operation -> !(operation instanceof ExtendedValueOperation))
        .toList());
    final List<PolicyOperation> validated = new ArrayList<>();
    if (!others.isEmpty()) {
      validated.addAll(this.delegate.validate(others));
    }
    final Map<String, Object> configurations = new HashMap<>();
    for (final PolicyOperation operation : policyOperations) {
      configurations.put(operation.getOperationName().getValue(), operation.toJSONObjectEntry().getValue());
      if (operation instanceof ExtendedValueOperation) {
        validated.add(operation);
      }
    }
    this.validateCombinations(configurations);
    validated.sort(Comparator.comparingInt(ExtendedPolicyOperationCombinationValidator::rank));
    return validated;
  }

  private void validateCombinations(final Map<String, Object> configurations) throws PolicyViolationException {
    if (configurations.containsKey(VALUE)) {
      final Object value = configurations.get(VALUE);
      final List<?> values = JsonValues.asList(value);
      if (configurations.containsKey(ADD)
          && (value == null || !JsonValues.containsAll(values, JsonValues.asList(configurations.get(ADD))))) {
        throw this.combinationError("add must be a subset of value");
      }
      if (configurations.containsKey(DEFAULT) && value == null) {
        throw this.combinationError("default cannot be combined with a null value");
      }
      if (configurations.containsKey(ONE_OF) && value != null
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
      if (value == null && Boolean.TRUE.equals(configurations.get(ESSENTIAL))) {
        throw this.combinationError("a null value cannot be combined with essential true");
      }
    }
    if (configurations.containsKey(ONE_OF)) {
      final Optional<String> other = Stream.of(ADD, SUBSET_OF, SUPERSET_OF)
          .filter(configurations::containsKey)
          .findFirst();
      if (other.isPresent()) {
        throw this.combinationError("one_of cannot be combined with " + other.get());
      }
    }
  }

  private PolicyViolationException combinationError(final String message) {
    return new PolicyViolationException("Invalid policy operation combination: " + message);
  }

  private static int rank(final PolicyOperation operation) {
    final int index = ORDER.indexOf(operation.getOperationName().getValue());
    if (index < 0) {
      return ORDER.size();
    }
    return index;
  }
}
