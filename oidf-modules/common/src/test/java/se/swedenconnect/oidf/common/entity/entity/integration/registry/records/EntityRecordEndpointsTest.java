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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests for the endpoint URLs of {@link EntityRecord}.
 *
 * @author Martin Lindström
 */
class EntityRecordEndpointsTest {

  @Test
  void trailingSlashIsRemovedBeforeAddingPaths() {
    final EntityRecord record = EntityRecord.builder()
        .entityIdentifier(new EntityID("https://example.com/"))
        .metadata(Map.of("federation_entity", Map.of("federation_fetch_endpoint", "/fetch")))
        .build();

    Assertions.assertTrue(record.getEntityConfigurationEndpoints()
        .contains("https://example.com/.well-known/openid-federation"));
    Assertions.assertTrue(record.getEntityConfigurationEndpoints().stream().noneMatch(e -> e.contains("//.well")));
    Assertions.assertEquals("https://example.com/fetch", record.getFederationFetchEndpoint().orElseThrow());
  }

  @Test
  void entityIdentifierWithoutTrailingSlashIsUnchanged() {
    final EntityRecord record = EntityRecord.builder()
        .entityIdentifier(new EntityID("https://example.com/entity"))
        .build();

    Assertions.assertTrue(record.getEntityConfigurationEndpoints()
        .contains("https://example.com/entity/.well-known/openid-federation"));
  }
}
