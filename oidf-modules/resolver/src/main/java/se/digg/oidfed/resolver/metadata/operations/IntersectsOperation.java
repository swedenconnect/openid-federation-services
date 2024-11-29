/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.resolver.metadata.operations;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONUtils;
import com.nimbusds.openid.connect.sdk.federation.policy.language.OperationName;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import com.nimbusds.openid.connect.sdk.federation.policy.language.StringListOperation;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of intersects policy operator according to <a
 * href="https://github.com/oidc-sweden/specifications/blob/main/swedish-oidc-fed-profile.md#511-intersects">...</a>
 *
 * @author Felix Hellman
 */
public class IntersectsOperation implements StringListOperation {
  /**
   * Operation name for this operation.
   */
  public static final OperationName OPERATION_NAME = new OperationName("intersects");

  private final Set<String> set = new HashSet<>();

  @Override
  public List<String> apply(final List<String> value) throws PolicyViolationException {

    if (value.stream().noneMatch(this.set::contains)) {
      throw new PolicyViolationException("values:%s does not intersect with set:%s".formatted(value, this.set));
    }

    return value;
  }

  @Override
  public OperationName getOperationName() {
    return OPERATION_NAME;
  }

  @Override
  public void parseConfiguration(final Object o) throws ParseException {
    final List<String> configuration = JSONUtils.toStringList(o);
    this.set.addAll(configuration);
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    return new AbstractMap.SimpleImmutableEntry<>(this.getOperationName().getValue(), this.set);
  }

  @Override
  public PolicyOperation merge(final PolicyOperation policyOperation) throws PolicyViolationException {
    if (policyOperation instanceof IntersectsOperation intersectsOperation) {
      this.set.retainAll(intersectsOperation.set);
    }
    return this;
  }
}
