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

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The {@code one_of} operator of OpenID Federation 1.0, Section 6.1.3.1.4, for strings, numbers and booleans.
 *
 * @author Felix Hellman
 */
public class ExtendedOneOfOperation implements ExtendedValueOperation {

  /** The operation name. */
  public static final OperationName NAME = new OperationName("one_of");

  private List<Object> values = List.of();

  /**
   * Configures the allowed values.
   *
   * @param values the allowed values
   */
  public void configure(final List<?> values) {
    this.values = new ArrayList<>(values);
  }

  /**
   * Gets the allowed values.
   *
   * @return the allowed values
   */
  public List<Object> getValues() {
    return new ArrayList<>(this.values);
  }

  @Override
  public OperationName getOperationName() {
    return NAME;
  }

  @Override
  public void parseConfiguration(final Object jsonEntity) throws ParseException {
    if (!(jsonEntity instanceof final List<?> list)) {
      throw new ParseException("The one_of configuration must be a JSON array");
    }
    final boolean allScalars = list.stream()
        .allMatch(item -> item instanceof String || item instanceof Number || item instanceof Boolean);
    if (!allScalars) {
      throw new ParseException("The one_of items must be strings, numbers or booleans");
    }
    this.configure(list);
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    return new AbstractMap.SimpleEntry<>(NAME.getValue(), new ArrayList<>(this.values));
  }

  @Override
  public PolicyOperation merge(final PolicyOperation other) throws PolicyViolationException {
    if (!(other instanceof final ExtendedOneOfOperation otherOneOf)) {
      throw new PolicyViolationException("Cannot merge one_of with " + other.getOperationName().getValue());
    }
    final List<Object> intersection = this.values.stream()
        .filter(item -> JsonValues.contains(otherOneOf.values, item))
        .toList();
    if (intersection.isEmpty()) {
      throw new PolicyViolationException("Merging one_of values %s and %s gives an empty set"
          .formatted(this.values, otherOneOf.values));
    }
    final ExtendedOneOfOperation merged = new ExtendedOneOfOperation();
    merged.configure(intersection);
    return merged;
  }

  @Override
  public Object apply(final Object value) throws PolicyViolationException {
    if (value == null) {
      return null;
    }
    if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
      throw new PolicyViolationException("one_of requires a single value");
    }
    if (!JsonValues.contains(this.values, value)) {
      throw new PolicyViolationException("%s is not one of %s".formatted(value, this.values));
    }
    return value;
  }
}
