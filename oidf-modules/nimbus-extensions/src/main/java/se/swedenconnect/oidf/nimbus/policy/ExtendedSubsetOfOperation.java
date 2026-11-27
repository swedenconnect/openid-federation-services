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
import java.util.List;
import java.util.Map;

/**
 * The {@code subset_of} operator of OpenID Federation 1.0, Section 6.1.3.1.5, for arrays of strings, numbers and JSON
 * objects.
 * <p>
 * A present metadata parameter is assigned the intersection of its values and the operator values, and the
 * intersection may be an empty array. The {@code subset_of} operator of the Nimbus SDK instead removes the parameter
 * when the intersection is empty.
 * </p>
 *
 * @author Martin Lindström
 */
public class ExtendedSubsetOfOperation implements ExtendedValueOperation {

  /** The operation name. */
  public static final OperationName NAME = new OperationName("subset_of");

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
      throw new ParseException("The subset_of configuration must be a JSON array");
    }
    final boolean allowedItems = list.stream()
        .allMatch(item -> item instanceof String || item instanceof Number || item instanceof Map<?, ?>);
    if (!allowedItems) {
      throw new ParseException("The subset_of items must be strings, numbers or objects");
    }
    this.configure(list);
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    return new AbstractMap.SimpleEntry<>(NAME.getValue(), new ArrayList<>(this.values));
  }

  /**
   * Merges this operation with another {@code subset_of} operation. The result is the intersection of the values,
   * which may be empty.
   */
  @Override
  public PolicyOperation merge(final PolicyOperation other) throws PolicyViolationException {
    if (!(other instanceof final ExtendedSubsetOfOperation otherSubsetOf)) {
      throw new PolicyViolationException("Cannot merge subset_of with " + other.getOperationName().getValue());
    }
    final ExtendedSubsetOfOperation merged = new ExtendedSubsetOfOperation();
    merged.configure(JsonValues.intersection(this.values, otherSubsetOf.values));
    return merged;
  }

  /**
   * Assigns the intersection of the parameter values and the configured values, possibly an empty array. An absent
   * parameter stays absent.
   */
  @Override
  public Object apply(final Object value) throws PolicyViolationException {
    if (value == null) {
      return null;
    }
    if (!(value instanceof final List<?> list)) {
      throw new PolicyViolationException("subset_of requires an array");
    }
    return JsonValues.intersection(list, this.values);
  }
}
