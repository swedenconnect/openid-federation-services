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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The metadata policy for one metadata parameter, holding the operators and their values. Implements parsing,
 * merging, combination checks and application of the standard operators of OpenID Federation 1.0, Section 6.1.3.1,
 * and the additional operators {@code regexp} and {@code intersects} of the Swedish OpenID Federation profile.
 *
 * @author Martin Lindström
 */
public final class ParameterPolicy {

  /** The {@code value} operator. */
  public static final String VALUE = "value";

  /** The {@code add} operator. */
  public static final String ADD = "add";

  /** The {@code default} operator. */
  public static final String DEFAULT = "default";

  /** The {@code one_of} operator. */
  public static final String ONE_OF = "one_of";

  /** The {@code subset_of} operator. */
  public static final String SUBSET_OF = "subset_of";

  /** The {@code superset_of} operator. */
  public static final String SUPERSET_OF = "superset_of";

  /** The {@code regexp} operator of the Swedish OpenID Federation profile. */
  public static final String REGEXP = "regexp";

  /** The {@code intersects} operator of the Swedish OpenID Federation profile. */
  public static final String INTERSECTS = "intersects";

  /** The {@code essential} operator. */
  public static final String ESSENTIAL = "essential";

  /**
   * The supported operators, in the order they are applied. The additional value checks are applied before
   * {@code essential}, as required by Section 6.1.3.2.
   */
  public static final List<String> OPERATORS =
      List.of(VALUE, ADD, DEFAULT, ONE_OF, SUBSET_OF, SUPERSET_OF, REGEXP, INTERSECTS, ESSENTIAL);

  /** The {@code scope} parameter is a space separated string that operators handle as an array (Section 6.1.3.1.8). */
  private static final String SCOPE = "scope";

  private final String parameter;

  private final Map<String, Object> operators;

  private ParameterPolicy(final String parameter, final Map<String, Object> operators)
      throws MetadataPolicyException {
    this.parameter = parameter;
    this.operators = operators;
    this.validateCombinations();
  }

  /**
   * Tells whether an operator is supported.
   *
   * @param operator the operator name
   * @return true if the operator is supported
   */
  public static boolean isSupported(final String operator) {
    return OPERATORS.contains(operator);
  }

  /**
   * Parses the policy of a metadata parameter.
   *
   * @param parameter the metadata parameter name
   * @param operators the operators and their values, all of which must be supported
   * @return the parameter policy
   * @throws MetadataPolicyException if an operator is not supported, an operator value has the wrong type, or the
   *     operators cannot be combined
   */
  public static ParameterPolicy parse(final String parameter, final Map<String, ?> operators)
      throws MetadataPolicyException {
    final Map<String, Object> parsed = new LinkedHashMap<>();
    for (final Map.Entry<String, ?> operator : operators.entrySet()) {
      if (!isSupported(operator.getKey())) {
        throw new MetadataPolicyException("Unsupported policy operator '%s' for '%s'"
            .formatted(operator.getKey(), parameter));
      }
      parsed.put(operator.getKey(), parseValue(parameter, operator.getKey(), operator.getValue()));
    }
    return new ParameterPolicy(parameter, parsed);
  }

  /**
   * Merges the policy of a subordinate statement into this policy (Section 6.1.4.1).
   *
   * @param subordinate the policy from the subordinate statement
   * @return the merged policy
   * @throws MetadataPolicyException if operator values cannot be merged, or the merged operators cannot be combined
   */
  public ParameterPolicy merge(final ParameterPolicy subordinate) throws MetadataPolicyException {
    final Map<String, Object> merged = new LinkedHashMap<>(this.operators);
    for (final Map.Entry<String, Object> operator : subordinate.operators.entrySet()) {
      final String name = operator.getKey();
      if (merged.containsKey(name)) {
        merged.put(name, this.mergeValues(name, merged.get(name), operator.getValue()));
      }
      else {
        merged.put(name, operator.getValue());
      }
    }
    return new ParameterPolicy(this.parameter, merged);
  }

  /**
   * Applies the policy to a metadata parameter value.
   *
   * @param input the metadata parameter value, null if the parameter is absent
   * @return the resulting value, null if the parameter is to be removed or stays absent
   * @throws MetadataPolicyException if a value check fails or the value has a type an operator does not support
   */
  public Object apply(final Object input) throws MetadataPolicyException {
    Object value = SCOPE.equals(this.parameter) && input instanceof final String scope ? splitScope(scope) : input;
    for (final String name : OPERATORS) {
      if (this.operators.containsKey(name)) {
        value = this.applyOperator(name, this.operators.get(name), value);
      }
    }
    if (SCOPE.equals(this.parameter) && value instanceof final List<?> scopes
        && scopes.stream().allMatch(String.class::isInstance)) {
      return String.join(" ", scopes.stream().map(String.class::cast).toList());
    }
    return value;
  }

  private static Object parseValue(final String parameter, final String operator, final Object value)
      throws MetadataPolicyException {
    final Object normalized = SCOPE.equals(parameter) && value instanceof final String scope
        ? splitScope(scope)
        : value;
    return switch (operator) {
      case VALUE -> copy(normalized);
      case DEFAULT -> {
        if (normalized == null) {
          throw new MetadataPolicyException("default for '%s' must not be null".formatted(parameter));
        }
        yield copy(normalized);
      }
      case ADD, ONE_OF, SUBSET_OF, SUPERSET_OF, INTERSECTS -> {
        if (!(normalized instanceof final List<?> values)) {
          throw new MetadataPolicyException("%s for '%s' must be an array".formatted(operator, parameter));
        }
        yield new ArrayList<Object>(values);
      }
      case ESSENTIAL -> {
        if (!(normalized instanceof Boolean)) {
          throw new MetadataPolicyException("essential for '%s' must be a boolean".formatted(parameter));
        }
        yield normalized;
      }
      case REGEXP -> parseRegexp(parameter, value);
      default -> throw new MetadataPolicyException("Unsupported policy operator '%s'".formatted(operator));
    };
  }

  private static List<Object> parseRegexp(final String parameter, final Object value)
      throws MetadataPolicyException {
    final List<?> patterns = value instanceof final List<?> list ? list : Arrays.asList(value);
    for (final Object pattern : patterns) {
      if (!(pattern instanceof final String regexp)) {
        throw new MetadataPolicyException("regexp for '%s' must be a string or an array of strings"
            .formatted(parameter));
      }
      try {
        Pattern.compile(regexp);
      }
      catch (final PatternSyntaxException e) {
        throw new MetadataPolicyException("regexp '%s' for '%s' is not valid".formatted(regexp, parameter));
      }
    }
    return new ArrayList<>(patterns);
  }

  private Object mergeValues(final String operator, final Object current, final Object subordinate)
      throws MetadataPolicyException {
    return switch (operator) {
      case VALUE, DEFAULT -> {
        if (!Objects.equals(current, subordinate)) {
          throw new MetadataPolicyException("Cannot merge %s values %s and %s for '%s'"
              .formatted(operator, current, subordinate, this.parameter));
        }
        yield current;
      }
      case ADD, SUPERSET_OF, REGEXP -> union(asList(current), asList(subordinate));
      case ONE_OF -> {
        final List<Object> intersection = intersection(asList(current), asList(subordinate));
        if (intersection.isEmpty()) {
          throw new MetadataPolicyException("Merging one_of values %s and %s for '%s' gives an empty set"
              .formatted(current, subordinate, this.parameter));
        }
        yield intersection;
      }
      case SUBSET_OF, INTERSECTS -> intersection(asList(current), asList(subordinate));
      case ESSENTIAL -> (Boolean) current || (Boolean) subordinate;
      default -> throw new MetadataPolicyException("Unsupported policy operator '%s'".formatted(operator));
    };
  }

  /**
   * Checks that the operators may be combined (Section 6.1.3.1).
   *
   * @throws MetadataPolicyException if the combination is not allowed
   */
  private void validateCombinations() throws MetadataPolicyException {
    if (this.operators.containsKey(VALUE)) {
      final Object value = this.operators.get(VALUE);
      final List<?> values = asList(value);
      if (this.operators.containsKey(ADD) && (value == null || !values.containsAll(this.list(ADD)))) {
        this.combinationError("add must be a subset of value");
      }
      if (this.operators.containsKey(DEFAULT) && value == null) {
        this.combinationError("default cannot be combined with a null value");
      }
      if (this.operators.containsKey(ONE_OF) && value != null && !this.list(ONE_OF).contains(value)) {
        this.combinationError("value must be among the one_of values");
      }
      if (this.operators.containsKey(SUBSET_OF) && !this.list(SUBSET_OF).containsAll(values)) {
        this.combinationError("value must be a subset of subset_of");
      }
      if (this.operators.containsKey(SUPERSET_OF) && !values.containsAll(this.list(SUPERSET_OF))) {
        this.combinationError("value must be a superset of superset_of");
      }
      if (value == null && Boolean.TRUE.equals(this.operators.get(ESSENTIAL))) {
        this.combinationError("a null value cannot be combined with essential true");
      }
    }
    if (this.operators.containsKey(ONE_OF)) {
      for (final String other : List.of(ADD, SUBSET_OF, SUPERSET_OF)) {
        if (this.operators.containsKey(other)) {
          this.combinationError("one_of cannot be combined with " + other);
        }
      }
    }
    if (this.operators.containsKey(ADD) && this.operators.containsKey(SUBSET_OF)
        && !this.list(SUBSET_OF).containsAll(this.list(ADD))) {
      this.combinationError("add must be a subset of subset_of");
    }
    if (this.operators.containsKey(SUBSET_OF) && this.operators.containsKey(SUPERSET_OF)
        && !this.list(SUBSET_OF).containsAll(this.list(SUPERSET_OF))) {
      this.combinationError("subset_of must be a superset of superset_of");
    }
  }

  private void combinationError(final String message) throws MetadataPolicyException {
    throw new MetadataPolicyException("Invalid policy for '%s': %s".formatted(this.parameter, message));
  }

  private Object applyOperator(final String operator, final Object config, final Object value)
      throws MetadataPolicyException {
    switch (operator) {
      case VALUE:
        return copy(config);
      case ADD:
        if (value == null) {
          return new ArrayList<>(asList(config));
        }
        final List<Object> added = new ArrayList<>(this.requireArray(operator, value));
        for (final Object item : asList(config)) {
          if (!added.contains(item)) {
            added.add(item);
          }
        }
        return added;
      case DEFAULT:
        return value == null ? copy(config) : value;
      case ONE_OF:
        if (value != null) {
          if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            throw this.applyError(operator, "parameter must be a single value");
          }
          if (!asList(config).contains(value)) {
            throw this.applyError(operator, "%s is not one of %s".formatted(value, config));
          }
        }
        return value;
      case SUBSET_OF:
        if (value == null) {
          return null;
        }
        return new ArrayList<>(this.requireArray(operator, value).stream().filter(asList(config)::contains).toList());
      case SUPERSET_OF:
        if (value != null && !this.requireArray(operator, value).containsAll(asList(config))) {
          throw this.applyError(operator, "%s does not contain all of %s".formatted(value, config));
        }
        return value;
      case REGEXP:
        if (value != null) {
          for (final Object item : asList(value)) {
            for (final Object regexp : asList(config)) {
              if (!(item instanceof final String text) || !Pattern.compile((String) regexp).matcher(text).matches()) {
                throw this.applyError(operator, "%s does not match %s".formatted(item, regexp));
              }
            }
          }
        }
        return value;
      case INTERSECTS:
        if (value != null && this.requireArray(operator, value).stream().noneMatch(asList(config)::contains)) {
          throw this.applyError(operator, "%s has no value in common with %s".formatted(value, config));
        }
        return value;
      case ESSENTIAL:
        if (value == null && Boolean.TRUE.equals(config)) {
          throw this.applyError(operator, "parameter is missing");
        }
        return value;
      default:
        throw new MetadataPolicyException("Unsupported policy operator '%s'".formatted(operator));
    }
  }

  private List<?> requireArray(final String operator, final Object value) throws MetadataPolicyException {
    if (!(value instanceof final List<?> list)) {
      throw this.applyError(operator, "parameter must be an array");
    }
    return list;
  }

  private MetadataPolicyException applyError(final String operator, final String message) {
    return new MetadataPolicyException("Policy operator %s failed for '%s': %s"
        .formatted(operator, this.parameter, message));
  }

  private List<?> list(final String operator) {
    return asList(this.operators.get(operator));
  }

  private static List<?> asList(final Object value) {
    if (value == null) {
      return List.of();
    }
    return value instanceof final List<?> list ? list : List.of(value);
  }

  private static List<Object> union(final List<?> first, final List<?> second) {
    final List<Object> union = new ArrayList<>(first);
    second.stream().filter(item -> !union.contains(item)).forEach(union::add);
    return union;
  }

  private static List<Object> intersection(final List<?> first, final List<?> second) {
    return new ArrayList<>(first.stream().filter(second::contains).toList());
  }

  private static Object copy(final Object value) {
    return value instanceof final List<?> list ? new ArrayList<Object>(list) : value;
  }

  private static List<Object> splitScope(final String scope) {
    return new ArrayList<>(Arrays.stream(scope.trim().split("\\s+")).filter(s -> !s.isEmpty()).toList());
  }
}
