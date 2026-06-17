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
package se.swedenconnect.oidf.resolver;

import org.mockito.Mockito;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.IM_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.LEAF_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.SAML_SP_ID;
import static se.swedenconnect.oidf.resolver.TestEntitiesFactory.TA_ID;

public class FederationClientMockFactory {

  public static FederationClient create(final TestEntitiesFactory entities) {
    final FederationClient client = Mockito.mock(FederationClient.class);

    when(client.entityConfiguration(any())).thenAnswer(inv -> {
      final FederationRequest<EntityConfigurationRequest> req = inv.getArgument(0);
      final String id = req.parameters().entityID().getValue();
      if (id.equals(TA_ID)) return entities.taEC;
      if (id.equals(IM_ID)) return entities.imEC;
      if (id.equals(LEAF_ID)) return entities.leafEC;
      if (id.equals(SAML_SP_ID)) return entities.samlSpEC;
      throw new RuntimeException("Unknown entity: " + id);
    });

    when(client.subordinateListing(any())).thenAnswer(inv -> {
      final FederationRequest<SubordinateListingRequest> req = inv.getArgument(0);
      final String listEndpoint = (String) req.federationEntityMetadata().get("federation_list_endpoint");
      if (listEndpoint != null && listEndpoint.startsWith(TA_ID)) return List.of(IM_ID);
      if (listEndpoint != null && listEndpoint.startsWith(IM_ID)) return List.of(LEAF_ID, SAML_SP_ID);
      return List.of();
    });

    when(client.fetch(any())).thenAnswer(inv -> {
      final FederationRequest<FetchRequest> req = inv.getArgument(0);
      final String subject = req.parameters().subject();
      if (subject.equals(IM_ID)) return entities.taImSub;
      if (subject.equals(LEAF_ID)) return entities.imLeafSub;
      if (subject.equals(SAML_SP_ID)) return entities.imSamlSpSub;
      throw new RuntimeException("Unknown fetch subject: " + subject);
    });

    return client;
  }
}
