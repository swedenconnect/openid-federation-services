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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for the JSON form of {@link ConstraintRecord} and {@link NamingConstraints}.
 *
 * @author Martin Lindström
 */
class ConstraintRecordTest {

  @Test
  void unsetNamingMembersAreNotWritten() {
    final Map<String, Object> json = NamingConstraints.builder().permitted(List.of(".example.com")).build().toJson();
    Assertions.assertEquals(Map.of("permitted", List.of(".example.com")), json);
  }

  @Test
  void federationEntityIsLeftOutOfAllowedEntityTypes() {
    final Map<String, Object> json = ConstraintRecord.builder()
        .allowedEntityTypes(List.of("federation_entity", "openid_provider"))
        .build()
        .toJson();
    Assertions.assertEquals(List.of("openid_provider"), json.get("allowed_entity_types"));
  }
}
