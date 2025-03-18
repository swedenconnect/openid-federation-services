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
package se.swedenconnect.oidf.service.entity;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;
import se.swedenconnect.oidf.service.suites.Context;

import static se.swedenconnect.oidf.service.entity.RegistryMock.RP_FROM_REGISTRY_ENTITY;


@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class EntityRegistryMockTestCases {

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

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
        clients.entity().getEntityConfiguration(RP_FROM_REGISTRY_ENTITY);
    Assertions.assertEquals(RP_FROM_REGISTRY_ENTITY, dynamicallyRegistered.getEntityID());
    Assertions.assertNotNull(dynamicallyRegistered);
  }
}
