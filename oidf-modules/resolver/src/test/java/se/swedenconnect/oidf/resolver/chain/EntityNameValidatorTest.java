/*
 * Copyright 2024-2025 Sweden Connect
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
  void testValidExamples() {
    List.of("https://test.example.com", "https://beef.example.com/stuff")
        .forEach(entity -> {
          EntityNameValidator.validate(entity, "https://.example.com");
        });
  }

  @Test
  void testFailingExamples() {
    List.of("https://test.eexample.com")
        .forEach(entity -> {
          Assertions.assertFalse(EntityNameValidator.validate(entity, "https://.example.com")
          );
        });

    List.of("https://test.example.com")
        .forEach(entity -> {
          Assertions.assertFalse(EntityNameValidator.validate(entity, "https://.example.com/path"));
        });
  }
}