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
package se.digg.oidfed.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JsonObjectPropertyTest {

  @Test
  void parseFile() {
    final JsonObjectProperty property = new JsonObjectProperty(null, new ClassPathResource("test-policy.json"));
    Assertions.assertTrue(property.toJsonObject().containsKey("openid_relying_party"), "Missing expected value " +
        "openid_relying_party");
  }

  @Test
  void parseJson() {
    final String json = """
        {
          "openid_relying_party": {
            "id_token_signed_response_alg": {
              "default": "ES256",
              "one_of": ["ES256", "ES384", "ES512"]
            }
          }
        }""";
    final JsonObjectProperty property = new JsonObjectProperty(json, null);
    Assertions.assertTrue(property.toJsonObject().containsKey("openid_relying_party"), "Missing expected value " +
        "openid_relying_party");
  }

  @Test
  void throwsOnBothFieldsNull() {
    final JsonObjectProperty property = new JsonObjectProperty(null, null);
    Assertions.assertThrows(IllegalArgumentException.class, property::validate);
  }

}