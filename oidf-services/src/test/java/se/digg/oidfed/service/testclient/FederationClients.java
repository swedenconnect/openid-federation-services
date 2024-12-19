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
package se.digg.oidfed.service.testclient;

import org.springframework.web.client.RestClient;
import se.digg.oidfed.service.entity.TestFederationEntities;

public class FederationClients {

  private final RestClient client;

  public FederationClients(final RestClient client) {
    this.client = client;
  }

  public TestFederationClient authorization() {
    return new TestFederationClient(
        client,
        TestFederationEntities.Authorization.RESOLVER,
        TestFederationEntities.Authorization.TRUST_ANCHOR,
        TestFederationEntities.Authorization.TRUST_MARK_ISSUER
    );
  }

  public TestFederationClient municipality() {
    return new TestFederationClient(
        client,
        TestFederationEntities.Municipality.RESOLVER,
        TestFederationEntities.Municipality.TRUST_ANCHOR,
        TestFederationEntities.Municipality.TRUST_MARK_ISSUER
    );
  }

  public TestFederationClient privateSector() {
    return new TestFederationClient(
        client,
        TestFederationEntities.PrivateSector.RESOLVER,
        TestFederationEntities.PrivateSector.TRUST_ANCHOR,
        TestFederationEntities.PrivateSector.TRUST_MARK_ISSUER
    );
  }

  public EntityClient entity() {
    return new EntityClient(client);
  }
}
