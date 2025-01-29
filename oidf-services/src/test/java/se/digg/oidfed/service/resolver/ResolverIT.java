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
package se.digg.oidfed.service.resolver;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.digg.oidfed.service.IntegrationTestParent;
import se.digg.oidfed.service.entity.TestFederationEntities;
import se.digg.oidfed.service.testclient.FederationClients;

class ResolverIT extends IntegrationTestParent {


  @Test
  void resolveFederation(final FederationClients clients) throws InterruptedException {
    final SignedJWT resolve = clients.municipality().resolver()
        .resolve(TestFederationEntities.Authorization.OP_1, TestFederationEntities.Municipality.TRUST_ANCHOR, null);

    Assertions.assertNotNull(resolve);
  }
}