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
package se.swedenconnect.oidf.service.service.testclient;

import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.resolver.ResolverDifferentiator;

public class FederationClients {

  private final RestClient client;

  public FederationClients(final RestClient client) {
    this.client = client;
  }

  public TestFederationClient anarchy() {
    return new TestFederationClient(
        client,
        TestFederationEntities.Anarchy.RESOLVER,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        TestFederationEntities.IM.TRUST_MARK_ISSUER
    );
  }

  public TrustAnchorClient intermediate() {
    return new TrustAnchorClient(client, TestFederationEntities.IM.INTERMEDIATE);
  }

  public ResolverDifferentiator policy() {
    return new ResolverDifferentiator(client,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        TestFederationEntities.Anarchy.RESOLVER,
        TestFederationEntities.Policy.TRUST_ANCHOR,
        TestFederationEntities.Policy.RESOLVER
    );
  }

  public ResolverDifferentiator crit() {
    return new ResolverDifferentiator(client,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        TestFederationEntities.Anarchy.RESOLVER,
        TestFederationEntities.Crit.TRUST_ANCHOR,
        TestFederationEntities.Crit.RESOLVER
    );
  }

  public ResolverDifferentiator metadataCrit() {
    return new ResolverDifferentiator(client,
        TestFederationEntities.Anarchy.TRUST_ANCHOR,
        TestFederationEntities.Anarchy.RESOLVER,
        TestFederationEntities.MetadataPolicyCrit.TRUST_ANCHOR,
        TestFederationEntities.MetadataPolicyCrit.RESOLVER
    );
  }

  public EntityClient entity() {
    return new EntityClient(client);
  }
}
