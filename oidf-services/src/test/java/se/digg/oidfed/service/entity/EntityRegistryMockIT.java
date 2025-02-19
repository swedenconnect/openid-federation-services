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
package se.digg.oidfed.service.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.HostedRecord;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.service.IntegrationTestParent;
import se.digg.oidfed.service.testclient.FederationClients;

import java.util.List;

@ActiveProfiles("integration-test")
@Slf4j
public class EntityRegistryMockIT extends IntegrationTestParent {

  @Autowired
  KeyRegistry registry;

  @Autowired
  EntityInitializer initializer;

  @Autowired
  EntityRecordRegistry entityRecordRegistry;

  @Test
  void test(final FederationClients clients) {
    final EntityStatement trustAnchor =
        clients.entity().getEntityConfiguration(TestFederationEntities.Authorization.TRUST_ANCHOR);

    Assertions.assertNotNull(trustAnchor);

    final EntityStatement municipalityTrustAnchor =
        clients.entity().getEntityConfiguration(TestFederationEntities.Municipality.TRUST_ANCHOR);

    Assertions.assertNotNull(municipalityTrustAnchor);

    final EntityStatement op_1 =
        clients.entity().getEntityConfiguration(TestFederationEntities.Authorization.OP_1);

    Assertions.assertNotNull(op_1);
  }

  @Test
  void dynamicRegistration(final FederationClients clients) {
    final EntityStatement dynamicallyRegistered =
        clients.entity().getEntityConfiguration(IntegrationTestParent.RP_FROM_REGISTRY_ENTITY);
    Assertions.assertEquals(RP_FROM_REGISTRY_ENTITY, dynamicallyRegistered.getEntityID());
    Assertions.assertNotNull(dynamicallyRegistered);
  }

  @Test
  void multiInserts(final FederationClients clients) {
    final String added = "https://municipality.local.swedenconnect.se/mystuff";
    for (int i = 0; i < 100; i++) {
      this.entityRecordRegistry.addEntity(EntityRecord.builder()
          .hostedRecord(HostedRecord.builder().build())
          .issuer(TestFederationEntities.Municipality.TRUST_ANCHOR)
          .subject(new EntityID(added))
          .build());
    }

    final List<String> subordinateListing = clients.municipality().trustAnchor().subordinateListing(SubordinateListingRequest.requestAll());
    Assertions.assertEquals(1, subordinateListing.stream().filter(sub -> sub.equals(added)).count());
  }
}
