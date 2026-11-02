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
import java.util.Map;

/**
 * The {@code value} operator of OpenID Federation 1.0, Section 6.1.3.1.1, for values of any JSON type, including
 * {@code null}. A {@code null} value removes the metadata parameter.
 *
 * @author Felix Hellman
 */
public class NullableValueOperation implements ExtendedValueOperation {

  /** The operation name. */
  public static final OperationName NAME = new OperationName("value");

  private Object value;

  /**
   * Configures the value.
   *
   * @param value the value, may be null
   */
  public void configure(final Object value) {
    this.value = JsonValues.copy(value);
  }

  /**
   * Gets the configured value.
   *
   * @return the value, may be null
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
    this.configure(jsonEntity);
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    return new AbstractMap.SimpleEntry<>(NAME.getValue(), JsonValues.copy(this.value));
  }

  @Override
  public PolicyOperation merge(final PolicyOperation other) throws PolicyViolationException {
    if (!(other instanceof final NullableValueOperation otherValue)) {
      throw new PolicyViolationException("Cannot merge value with " + other.getOperationName().getValue());
    }
    if (!JsonValues.equal(this.value, otherValue.value)) {
      throw new PolicyViolationException("Cannot merge value %s with %s".formatted(this.value, otherValue.value));
    }
    final NullableValueOperation merged = new NullableValueOperation();
    merged.configure(this.value);
    return merged;
  }

  @Override
  public Object apply(final Object value) {
    return JsonValues.copy(this.value);
  }
}
