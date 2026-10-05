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
package se.swedenconnect.oidf.resolver.chain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class EntityNameValidatorTest {

  @Test
  void domainConstraintMatchesSubdomains() {
    List.of("https://test.example.com", "https://beef.example.com/stuff", "https://my.host.example.com:8443/x")
        .forEach(entity -> Assertions.assertTrue(EntityNameValidator.validate(entity, ".example.com"), entity));
  }

  @Test
  void domainConstraintDoesNotMatchDomainItself() {
    Assertions.assertFalse(EntityNameValidator.validate("https://example.com", ".example.com"));
  }

  @Test
  void domainConstraintRequiresLabelBoundary() {
    Assertions.assertFalse(EntityNameValidator.validate("https://test.eexample.com", ".example.com"));
    Assertions.assertFalse(EntityNameValidator.validate("https://evilexample.com", ".example.com"));
  }

  @Test
  void hostConstraintMatchesHostOnly() {
    Assertions.assertTrue(EntityNameValidator.validate("https://host.example.com/path", "host.example.com"));
    Assertions.assertTrue(EntityNameValidator.validate("https://HOST.example.com", "host.EXAMPLE.com"));
    Assertions.assertFalse(EntityNameValidator.validate("https://sub.host.example.com", "host.example.com"));
    Assertions.assertFalse(EntityNameValidator.validate("https://evilhost.example.com", "host.example.com"));
  }

  @Test
  void uriFormattedConstraintMatchesNothing() {
    Assertions.assertFalse(EntityNameValidator.validate("https://example.com/path", "https://example.com"));
  }

  @Test
  void anyMatch() {
    Assertions.assertTrue(EntityNameValidator.anyMatch("https://a.example.com",
        List.of("other.com", ".example.com")));
    Assertions.assertFalse(EntityNameValidator.anyMatch("https://a.example.com", List.of("other.com")));
    Assertions.assertFalse(EntityNameValidator.anyMatch("https://a.example.com", List.of()));
  }
}
