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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import org.testcontainers.shaded.com.google.common.collect.MapDifference;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class ResolverTrustMarkTestCases {

  @Test
  void testTrustMarkIssuers() throws ParseException {
    final EntityID anarchyTrustAnchor = new EntityID("http://localhost:11111/anarchy/ta");
    final EntityID anarchyResolver = new EntityID("http://localhost:11111/anarchy/resolver");
    final EntityID resolver = new EntityID("http://localhost:11111/trust_mark_issuer/resolver");
    final EntityID trustAnchor = new EntityID("http://localhost:11111/trust_mark_issuer/ta");
    final ResolverDifferentiator differentiator = new ResolverDifferentiator(
        RestClient.builder().build(),
        anarchyTrustAnchor,
        anarchyResolver,
        trustAnchor,
        resolver
    );

    final ResolverDifferentiator.ResponseDifference difference = differentiator.getResponseDifference(new ResolveRequest("http://localhost:11111/im/op", "http://localhost:11111" +
        "/trust_mark_issuer/ta", null, false));

    final Map<String, MapDifference.ValueDifference<Object>> jsonDifference = difference.getJsonDifference();
    System.out.println(jsonDifference);
    List<String> left = (List<String>) difference.getJsonDifference().get("trust_marks").leftValue();
    List<String> right = (List<String>) difference.getJsonDifference().get("trust_marks").rightValue();
    Assertions.assertEquals(2, left.size());
    Assertions.assertEquals(1, right.size());
  }

  @Test
  void resolveTrustMarkOwner(final FederationClients clients) throws ParseException {
    final ResolverDifferentiator.ResponseDifference diff = clients.trustMarkOwners().getResponseDifference(
        new ResolveRequest(
            TestFederationEntities.IM.OP.getValue(),
            TestFederationEntities.TrustMarkOwner.TRUST_ANCHOR.getValue(),
            null,
            false
        )
    );
    final Map<String, MapDifference.ValueDifference<Object>> trustChainEntryDifference =
        diff.getTrustChainEntryDifference(3);
    Assertions.assertTrue(trustChainEntryDifference.containsKey("trust_mark_owners"));
  }
}
