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
import com.nimbusds.openid.connect.sdk.federation.policy.language.StringOperation;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Metadata policy operation that implements <a
 * href="https://github.com/oidc-sweden/specifications/blob/main/swedish-oidc-fed-profile.md#512-regexp">...</a>
 *
 * @author Felix Hellman
 */
public class RegexpOperation implements StringOperation {

  /**
   * Operation name for this operation.
   */
  public static final OperationName OPERATION_NAME = new OperationName("regexp");
  private final Set<Pattern> regexp = new HashSet<>();

  @Override
  public String apply(final String input) throws PolicyViolationException {
    final List<Matcher> list = this.regexp.stream().map(p -> p.matcher(input))
        .filter(matcher -> !matcher.matches())
        .toList();

    if (!list.isEmpty()) {
      final Stream<String> violatingPatterns = list.stream().map(matcher -> matcher.pattern().pattern());
      final String exceptionMessage = "Failed to validate input:%s towards following regexp:%s";
      throw new PolicyViolationException(exceptionMessage.formatted(input, violatingPatterns));
    }

    return input;
  }

  @Override
  public OperationName getOperationName() {
    return OPERATION_NAME;
  }

  @Override
  public void parseConfiguration(final Object configuration) throws ParseException {
    this.regexp.addAll(JSONUtils.toStringList(configuration).stream().map(Pattern::compile).toList());
  }

  @Override
  public Map.Entry<String, Object> toJSONObjectEntry() {
    final List<String> patterns = this.regexp.stream().map(Pattern::pattern).toList();
    return new AbstractMap.SimpleImmutableEntry<>(this.getOperationName().getValue(), patterns);
  }

  @Override
  public PolicyOperation merge(final PolicyOperation other) throws PolicyViolationException {
    if (other instanceof RegexpOperation otherRegexpOperation) {
      this.regexp.addAll(otherRegexpOperation.regexp);
    }
    else {
      throw new PolicyViolationException(
          "Can not merge different policy types %s with %s".formatted(this.getClass().getCanonicalName(),
              other.getClass().getCanonicalName()));
    }

    return this;
  }
}
