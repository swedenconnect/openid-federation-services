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
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyOperation;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ExtendedMetadataPolicyTest {

  private static final String RELYING_PARTY = "openid_relying_party";
  private static final String PROVIDER = "openid_provider";

  private static final String POLICY = """
      {
        "metadata_policy": {
          "openid_relying_party": {
            "token_endpoint_auth_method": {
              "essential": true,
              "one_of": [
                "private_key_jwt"
              ]
            },
            "subject_type": {
              "default": "public",
              "one_of": [
                "public",
                "pairwise"
              ]
            },
            "client_registration_types": {
              "value": null
            },
            "https://id.oidc.se/param/userMessageSupported": {
              "one_of": [
                true
              ]
            }
          },
          "openid_provider": {
            "code_challenge_methods_supported": {
              "essential": true,
              "subset_of": [
                "S256"
              ],
              "superset_of": [
                "S256"
              ]
            },
            "claims_parameter_supported": {
              "value": true
            },
            "request_parameter_supported": {
              "value": true
            },
            "request_uri_parameter_supported": {
              "essential": true
            },
            "client_registration_types_supported": {
              "default": [
                "automatic"
              ]
            }
          }
        }
      }
      """;

  @Test
  void stringOneOfIsParsed() throws Exception {
    final MetadataPolicy policy = parse(POLICY, RELYING_PARTY);
    final JSONObject parameter = (JSONObject) policy.toJSONObject().get("subject_type");
    Assertions.assertEquals(List.of("public", "pairwise"), parameter.get("one_of"));
    Assertions.assertEquals("public", parameter.get("default"));
  }

  @Test
  void booleanValueIsParsed() throws Exception {
    final MetadataPolicy policy = parse(POLICY, PROVIDER);
    final JSONObject parameter = (JSONObject) policy.toJSONObject().get("claims_parameter_supported");
    Assertions.assertEquals(true, parameter.get("value"));
  }

  @Test
  void oneOfWithBooleanIsParsed() throws Exception {
    final MetadataPolicy policy = parse(POLICY, RELYING_PARTY);
    final JSONObject parameter = (JSONObject) policy.toJSONObject().get("https://id.oidc.se/param/userMessageSupported");
    Assertions.assertEquals(List.of(true), parameter.get("one_of"));
  }

  @Test
  void nullValueIsParsedAndKeptInJson() throws Exception {
    final MetadataPolicy policy = parse(POLICY, RELYING_PARTY);
    final JSONObject parameter = (JSONObject) policy.toJSONObject().get("client_registration_types");
    Assertions.assertTrue(parameter.containsKey("value"));
    Assertions.assertNull(parameter.get("value"));
  }

  @Test
  void valueOutsideOneOfFails() {
    final String json = """
        {
          "metadata_policy": {
            "openid_provider": {
              "claims_parameter_supported": {
                "value": false,
                "one_of": [
                  true
                ]
              }
            }
          }
        }
        """;
    Assertions.assertThrows(PolicyViolationException.class, () -> parse(json, PROVIDER));
  }

  @Test
  void nullValueWithEssentialFails() {
    final String json = """
        {
          "metadata_policy": {
            "openid_provider": {
              "request_uri_parameter_supported": {
                "value": null,
                "essential": true
              }
            }
          }
        }
        """;
    Assertions.assertThrows(PolicyViolationException.class, () -> parse(json, PROVIDER));
  }

  @Test
  void oneOfWithObjectItemFails() {
    final String json = """
        {
          "metadata_policy": {
            "openid_relying_party": {
              "subject_type": {
                "one_of": [
                  {
                    "a": 1
                  }
                ]
              }
            }
          }
        }
        """;
    Assertions.assertThrows(ParseException.class, () -> parse(json, RELYING_PARTY));
  }

  @Test
  void oneOfAppliesToBooleansAndNumbers() throws Exception {
    final String json = """
        {
          "metadata_policy": {
            "openid_relying_party": {
              "https://id.oidc.se/param/userMessageSupported": {
                "one_of": [
                  true,
                  1
                ]
              }
            }
          }
        }
        """;
    final ExtendedOneOfOperation oneOf = operation(parse(json, RELYING_PARTY),
        "https://id.oidc.se/param/userMessageSupported", ExtendedOneOfOperation.class);
    Assertions.assertEquals(true, oneOf.apply(true));
    Assertions.assertEquals(1L, oneOf.apply(1L));
    Assertions.assertNull(oneOf.apply(null));
    Assertions.assertThrows(PolicyViolationException.class, () -> oneOf.apply(false));
    Assertions.assertThrows(PolicyViolationException.class, () -> oneOf.apply(List.of(true)));
  }

  @Test
  void oneOfMergeGivesIntersection() throws Exception {
    final String json = """
        {
          "metadata_policy": {
            "openid_relying_party": {
              "first": {
                "one_of": [
                  true,
                  false
                ]
              },
              "second": {
                "one_of": [
                  true
                ]
              },
              "third": {
                "one_of": [
                  "x"
                ]
              }
            }
          }
        }
        """;
    final MetadataPolicy policy = parse(json, RELYING_PARTY);
    final ExtendedOneOfOperation first = operation(policy, "first", ExtendedOneOfOperation.class);
    final ExtendedOneOfOperation second = operation(policy, "second", ExtendedOneOfOperation.class);
    final ExtendedOneOfOperation third = operation(policy, "third", ExtendedOneOfOperation.class);
    Assertions.assertEquals(List.of(true), ((ExtendedOneOfOperation) first.merge(second)).getValues());
    Assertions.assertThrows(PolicyViolationException.class, () -> second.merge(third));
  }

  @Test
  void nullValueRemovesParameterAndMergesWithNull() throws Exception {
    final String json = """
        {
          "metadata_policy": {
            "openid_relying_party": {
              "client_registration_types": {
                "value": null
              },
              "other": {
                "value": null
              },
              "not_null": {
                "value": "x"
              }
            }
          }
        }
        """;
    final MetadataPolicy policy = parse(json, RELYING_PARTY);
    final NullableValueOperation value = operation(policy, "client_registration_types", NullableValueOperation.class);
    final NullableValueOperation other = operation(policy, "other", NullableValueOperation.class);
    final NullableValueOperation notNull = operation(policy, "not_null", NullableValueOperation.class);
    Assertions.assertNull(value.apply("present"));
    Assertions.assertNull(((NullableValueOperation) value.merge(other)).getValue());
    Assertions.assertThrows(PolicyViolationException.class, () -> value.merge(notNull));
  }

  @Test
  void subsetOfWithNoCommonValuesGivesEmptyArray() throws Exception {
    final ExtendedSubsetOfOperation subsetOf = operation(
        parseParameter("{\"subset_of\": [\"a\", \"b\", \"c\"]}"), "p", ExtendedSubsetOfOperation.class);
    Assertions.assertEquals(List.of(), subsetOf.apply(List.of("d", "e")));
    Assertions.assertEquals(List.of("b"), subsetOf.apply(List.of("b", "d")));
    Assertions.assertNull(subsetOf.apply(null));
    Assertions.assertThrows(PolicyViolationException.class, () -> subsetOf.apply("a"));
  }

  @Test
  void subsetOfMergeMayGiveEmptySet() throws Exception {
    final MetadataPolicy policy = parse("""
        {
          "metadata_policy": {
            "openid_relying_party": {
              "first": { "subset_of": ["a", 1] },
              "second": { "subset_of": ["b", 1.0] },
              "third": { "subset_of": ["c"] }
            }
          }
        }
        """, RELYING_PARTY);
    final ExtendedSubsetOfOperation first = operation(policy, "first", ExtendedSubsetOfOperation.class);
    final ExtendedSubsetOfOperation second = operation(policy, "second", ExtendedSubsetOfOperation.class);
    final ExtendedSubsetOfOperation third = operation(policy, "third", ExtendedSubsetOfOperation.class);
    Assertions.assertEquals(List.of(1), ((ExtendedSubsetOfOperation) first.merge(second)).getValues());
    Assertions.assertEquals(List.of(), ((ExtendedSubsetOfOperation) first.merge(third)).getValues());
  }

  @Test
  void addOutsideSubsetOfFails() {
    Assertions.assertThrows(PolicyViolationException.class,
        () -> parseParameter("{\"add\": [\"c\"], \"subset_of\": [\"a\", \"b\"]}"));
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"add\": [\"a\"], \"subset_of\": [\"a\", \"b\"]}"));
  }

  @Test
  void defaultMayBeCombinedWithSubsetOfAndSupersetOf() {
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"default\": [\"c\"], \"subset_of\": [\"a\"]}"));
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"default\": [\"c\"], \"superset_of\": [\"a\"]}"));
  }

  @Test
  void nullValueMayBeCombinedWithValueChecks() {
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"value\": null, \"superset_of\": [\"a\"]}"));
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"value\": null, \"subset_of\": [\"a\"]}"));
    Assertions.assertDoesNotThrow(() -> parseParameter("{\"value\": null, \"one_of\": [\"a\"]}"));
  }

  @Test
  void subsetOfMustBeSupersetOfSupersetOf() {
    Assertions.assertThrows(PolicyViolationException.class,
        () -> parseParameter("{\"subset_of\": [\"a\"], \"superset_of\": [\"a\", \"b\"]}"));
  }

  @Test
  void defaultAcceptsNumbersAndBooleans() throws Exception {
    final ExtendedDefaultOperation number =
        operation(parseParameter("{\"default\": 3600}"), "p", ExtendedDefaultOperation.class);
    Assertions.assertEquals(3600, ((Number) number.apply(null)).intValue());
    Assertions.assertEquals(60L, number.apply(60L));
    final ExtendedDefaultOperation bool =
        operation(parseParameter("{\"default\": true}"), "p", ExtendedDefaultOperation.class);
    Assertions.assertEquals(true, bool.apply(null));
    Assertions.assertThrows(ParseException.class, () -> parseParameter("{\"default\": {\"a\": 1}}"));
  }

  @Test
  void defaultMergeRequiresEqualValues() throws Exception {
    final ExtendedDefaultOperation first =
        operation(parseParameter("{\"default\": 1}"), "p", ExtendedDefaultOperation.class);
    final ExtendedDefaultOperation second =
        operation(parseParameter("{\"default\": 1.0}"), "p", ExtendedDefaultOperation.class);
    final ExtendedDefaultOperation third =
        operation(parseParameter("{\"default\": 2}"), "p", ExtendedDefaultOperation.class);
    Assertions.assertEquals(1, ((Number) ((ExtendedDefaultOperation) first.merge(second)).getValue()).intValue());
    Assertions.assertThrows(PolicyViolationException.class, () -> first.merge(third));
  }

  private static MetadataPolicy parseParameter(final String operators) throws Exception {
    return ExtendedMetadataPolicy.parse(
        new JSONObject(java.util.Map.of("p", JSONValue.parse(operators))));
  }

  private static MetadataPolicy parse(final String json, final String entityType) throws Exception {
    final JSONObject metadataPolicy = (JSONObject) ((JSONObject) JSONValue.parse(json)).get("metadata_policy");
    return ExtendedMetadataPolicy.parse((JSONObject) metadataPolicy.get(entityType));
  }

  private static <T extends PolicyOperation> T operation(
      final MetadataPolicy policy, final String parameter, final Class<T> type) {
    return policy.getEntry(parameter).getPolicyOperations().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .findFirst()
        .orElseThrow();
  }
}
