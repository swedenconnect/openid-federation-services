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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ParameterPolicy}, following OpenID Federation 1.0, Section 6.1.3.
 *
 * @author Martin Lindström
 */
class ParameterPolicyTest {

  @Test
  void valueCombinedWithAllowedOperators() throws Exception {
    Assertions.assertEquals("x", policy("p", Map.of("value", "x", "one_of", List.of("x", "y"))).apply("y"));
    Assertions.assertEquals(List.of("a", "b"),
        policy("p", Map.of("value", List.of("a", "b"), "add", List.of("a"))).apply(null));
    Assertions.assertEquals(List.of("a"),
        policy("p", Map.of("value", List.of("a"), "subset_of", List.of("a", "b"))).apply(null));
    Assertions.assertEquals(List.of("a", "b"),
        policy("p", Map.of("value", List.of("a", "b"), "superset_of", List.of("a"))).apply(null));
    Assertions.assertEquals("x", policy("p", Map.of("value", "x", "default", "y")).apply(null));
  }

  @Test
  void valueCombinationsThatBreakTheConditionsFail() {
    assertPolicyError(Map.of("value", "z", "one_of", List.of("x", "y")));
    assertPolicyError(Map.of("value", List.of("a"), "add", List.of("b")));
    assertPolicyError(Map.of("value", List.of("c"), "subset_of", List.of("a", "b")));
    assertPolicyError(Map.of("value", List.of("a"), "superset_of", List.of("a", "b")));
  }

  @Test
  void nullValueRemovesParameter() throws Exception {
    final Map<String, Object> operators = new HashMap<>();
    operators.put("value", null);
    Assertions.assertNull(policy("p", operators).apply("existing"));
  }

  @Test
  void nullValueCannotBeCombinedWithDefaultOrEssential() {
    final Map<String, Object> withDefault = new HashMap<>();
    withDefault.put("value", null);
    withDefault.put("default", "x");
    assertPolicyError(withDefault);

    final Map<String, Object> withEssential = new HashMap<>();
    withEssential.put("value", null);
    withEssential.put("essential", true);
    assertPolicyError(withEssential);
  }

  @Test
  void subsetOfWithEssentialFollowsTable1() throws Exception {
    final ParameterPolicy essential = policy("p", Map.of("essential", true, "subset_of", List.of("a", "b", "c")));
    final ParameterPolicy voluntary = policy("p", Map.of("essential", false, "subset_of", List.of("a", "b", "c")));

    Assertions.assertEquals(List.of("a"), essential.apply(List.of("a", "e")));
    Assertions.assertEquals(List.of("a"), voluntary.apply(List.of("a", "e")));
    Assertions.assertEquals(List.of(), essential.apply(List.of("d", "e")));
    Assertions.assertEquals(List.of(), voluntary.apply(List.of("d", "e")));
    Assertions.assertThrows(MetadataPolicyException.class, () -> essential.apply(null));
    Assertions.assertNull(voluntary.apply(null));
  }

  @Test
  void oneOfWithEmptyIntersectionFailsToMerge() throws Exception {
    final ParameterPolicy first = policy("p", Map.of("one_of", List.of("a", "b")));
    final ParameterPolicy second = policy("p", Map.of("one_of", List.of("c")));
    Assertions.assertThrows(MetadataPolicyException.class, () -> first.merge(second));
  }

  @Test
  void subsetOfWithEmptyIntersectionMergesToEmptyArray() throws Exception {
    final ParameterPolicy merged = policy("p", Map.of("subset_of", List.of("a")))
        .merge(policy("p", Map.of("subset_of", List.of("b"))));
    Assertions.assertEquals(List.of(), merged.apply(List.of("a", "b")));
  }

  @Test
  void mergeRules() throws Exception {
    final ParameterPolicy merged = policy("p", Map.of("add", List.of("a"), "superset_of", List.of("a"),
        "essential", false))
        .merge(policy("p", Map.of("add", List.of("b"), "superset_of", List.of("b"), "essential", true)));
    Assertions.assertEquals(List.of("x", "a", "b"), merged.apply(List.of("x")));
    final ParameterPolicy essential = policy("p", Map.of("essential", false))
        .merge(policy("p", Map.of("essential", true)));
    Assertions.assertThrows(MetadataPolicyException.class, () -> essential.apply(null));

    final ParameterPolicy value = policy("p", Map.of("value", "x"));
    Assertions.assertThrows(MetadataPolicyException.class, () -> value.merge(policy("p", Map.of("value", "y"))));
    final ParameterPolicy defaults = policy("p", Map.of("default", "x"));
    Assertions.assertThrows(MetadataPolicyException.class, () -> defaults.merge(policy("p", Map.of("default", "y"))));
  }

  @Test
  void oneOfCannotBeCombinedWithSetOperators() {
    assertPolicyError(Map.of("one_of", List.of("a"), "subset_of", List.of("a")));
    assertPolicyError(Map.of("one_of", List.of("a"), "superset_of", List.of("a")));
    assertPolicyError(Map.of("one_of", List.of("a"), "add", List.of("a")));
  }

  @Test
  void setOperatorConditions() {
    assertPolicyError(Map.of("add", List.of("c"), "subset_of", List.of("a", "b")));
    assertPolicyError(Map.of("subset_of", List.of("a"), "superset_of", List.of("a", "b")));
  }

  @Test
  void scopeIsHandledAsArray() throws Exception {
    final ParameterPolicy policy = policy("scope", Map.of("subset_of", List.of("openid", "profile", "email")));
    Assertions.assertEquals("openid email", policy.apply("openid email phone"));

    final ParameterPolicy defaultScope = policy("scope", Map.of("default", "openid profile",
        "superset_of", List.of("openid")));
    Assertions.assertEquals("openid profile", defaultScope.apply(null));
  }

  @Test
  void wrongValueTypesFail() throws Exception {
    assertPolicyError(Map.of("subset_of", "a"));
    assertPolicyError(Map.of("essential", "yes"));
    final ParameterPolicy oneOf = policy("p", Map.of("one_of", List.of("a")));
    Assertions.assertThrows(MetadataPolicyException.class, () -> oneOf.apply(List.of("a")));
    final ParameterPolicy subsetOf = policy("p", Map.of("subset_of", List.of("a")));
    Assertions.assertThrows(MetadataPolicyException.class, () -> subsetOf.apply("a"));
  }

  private static ParameterPolicy policy(final String parameter, final Map<String, ?> operators)
      throws MetadataPolicyException {
    return ParameterPolicy.parse(parameter, operators);
  }

  private static void assertPolicyError(final Map<String, ?> operators) {
    Assertions.assertThrows(MetadataPolicyException.class, () -> ParameterPolicy.parse("p", operators),
        operators.toString());
  }
}
