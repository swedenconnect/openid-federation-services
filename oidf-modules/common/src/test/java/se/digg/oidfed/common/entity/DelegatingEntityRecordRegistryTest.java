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
package se.digg.oidfed.common.entity;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class DelegatingEntityRecordRegistryTest {
  @Test
  void testTest() {
    final EntityID root = new EntityID("http://root.test");
    final EntityID hostedSubject = new EntityID("http://root.test/sub");
    final EntityID selfHostedSubject = new EntityID("http://other.test/selfsub");
    final List<EntityRecord> recordsRegistred = new ArrayList<>();
    final DelegatingEntityRecordRegistry registry = new DelegatingEntityRecordRegistry(
        new InMemoryEntityRecordRegistry(new EntityPathFactory(List.of("http://root.test"))),
        List.of(recordsRegistred::add));

    final EntityRecord rootEntity = EntityRecord.builder()
        .issuer(root)
        .subject(root)
        .hostedRecord(HostedRecord.builder().build())
        .build();

    final EntityRecord first = EntityRecord.builder()
        .issuer(root)
        .subject(selfHostedSubject)
        .policyRecordId("my-policy")
        .build();
    final EntityRecord second = EntityRecord.builder()
        .issuer(root)
        .subject(hostedSubject)
        .policyRecordId("my-policy")
        .hostedRecord(HostedRecord.builder().build())
        .build();

    registry.addEntity(rootEntity);
    registry.addEntity(first);
    registry.addEntity(second);

    Assertions.assertTrue(registry.getEntity("/").isPresent());
    Assertions.assertTrue(registry.getEntity("/sub").isPresent());
    Assertions.assertEquals(hostedSubject, registry.getEntity("/sub").get().getSubject());
    Assertions.assertTrue(registry.getEntity("/selfsub").isEmpty());

    Assertions.assertTrue(registry.getEntity(selfHostedSubject).isPresent());
    Assertions.assertTrue(registry.getEntity(hostedSubject).isPresent());

    Assertions.assertEquals(3, recordsRegistred.size());
  }
}
