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
import java.util.List;
import java.util.Map;

/**
 * The {@code default} operator of OpenID Federation 1.0, Section 6.1.3.1.3, for strings, numbers, booleans and
 * arrays. The {@code default} operator of the Nimbus SDK does not accept numbers or arrays of other values than
 * strings.
 *
 * @author Martin Lindström
 */
public class ExtendedDefaultOperation implements ExtendedValueOperation {

  /** The operation name. */
  public static final OperationName NAME = new OperationName("default");

  private Object value;

  /**
   * Configures the default value.
   *
   * @param value the value (a string, number, boolean or array)
   */
  public void configure(final Object value) {
    this.value = JsonValues.copy(value);
  }

  /**
   * Gets the configured default value.
   *
   * @return the value
   */
  public Object getValue() {
    return JsonValues.copy(this.value);
  }

  @Override
  public OperationName getOperationName() {
    return NAME;
  }

  @Override
  public void parseConfiguration(final Object jsonEntity) throws ParseException {
    if (!(jsonEntity instanceof String || jsonEntity instanceof Number || jsonEntity instanceof Boolean
        || jsonEntity instanceof List<?>)) {
      throw new ParseException("The default configuration must be a string, number, boolean or array");
    }
    this.configure(jsonEntity);
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    return new AbstractMap.SimpleEntry<>(NAME.getValue(), JsonValues.copy(this.value));
  }

  /**
   * Merges this operation with another {@code default} operation. The values must be equal.
   */
  @Override
  public PolicyOperation merge(final PolicyOperation other) throws PolicyViolationException {
    if (!(other instanceof final ExtendedDefaultOperation otherDefault)) {
      throw new PolicyViolationException("Cannot merge default with " + other.getOperationName().getValue());
    }
    if (!JsonValues.equal(this.value, otherDefault.value)) {
      throw new PolicyViolationException("Cannot merge default %s with %s".formatted(this.value, otherDefault.value));
    }
    final ExtendedDefaultOperation merged = new ExtendedDefaultOperation();
    merged.configure(this.value);
    return merged;
  }

  /**
   * Returns the parameter value if present, otherwise the configured value.
   */
  @Override
  public Object apply(final Object value) {
    return value != null ? value : JsonValues.copy(this.value);
  }
}
