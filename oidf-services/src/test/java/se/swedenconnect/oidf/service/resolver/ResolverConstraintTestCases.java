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
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;

import java.text.ParseException;

@Slf4j
public class ResolverConstraintTestCases {
  @Test
  void testEntityTypesOnlyAllowRelyingParty() {
    final EntityID anarchyTrustAnchor = new EntityID("http://localhost:11111/anarchy/ta");
    final EntityID anarchyResolver = new EntityID("http://localhost:11111/anarchy/resolver");
    final EntityID resolver = new EntityID("http://localhost:11111/entity_type/resolver");
    final EntityID trustAnchor = new EntityID("http://localhost:11111/entity_type/ta");
    final ResolverDifferentiator differentiator = new ResolverDifferentiator(
        RestClient.builder().build(),
        anarchyTrustAnchor,
        anarchyResolver,
        trustAnchor,
        resolver
    );

    final ResolverDifferentiator.ResponseDifference difference = differentiator.getResponseDifference(
        new ResolveRequest(
            "http://localhost:11111/im/op",
            trustAnchor.getValue(),
            null
        )
    );
    Assertions.assertNull(difference.getReference().getError());
    Assertions.assertNotNull(difference.getResponse().getError());
    Assertions.assertEquals(400, difference.getResponse().getError().getStatusCode());
  }

  @Test
  void testMaxPathOnlyAllowsMaxDepth1() {
    final EntityID anarchyTrustAnchor = new EntityID("http://localhost:11111/anarchy/ta");
    final EntityID anarchyResolver = new EntityID("http://localhost:11111/anarchy/resolver");
    final EntityID resolver = new EntityID("http://localhost:11111/path/resolver");
    final EntityID trustAnchor = new EntityID("http://localhost:11111/path/ta");
    final ResolverDifferentiator differentiator = new ResolverDifferentiator(
        RestClient.builder().build(),
        anarchyTrustAnchor,
        anarchyResolver,
        trustAnchor,
        resolver
    );

    final ResolverDifferentiator.ResponseDifference difference = differentiator.getResponseDifference(
        new ResolveRequest(
            "http://localhost:11111/im/im/op",
            trustAnchor.getValue(),
            null
        )
    );
    Assertions.assertNull(difference.getReference().getError());
    Assertions.assertNotNull(difference.getResponse().getError());
    Assertions.assertEquals(400, difference.getResponse().getError().getStatusCode());
  }


  @Test
  void testNamingConstraints() throws ParseException {
    final EntityID anarchyTrustAnchor = new EntityID("http://localhost:11111/anarchy/ta");
    final EntityID anarchyResolver = new EntityID("http://localhost:11111/anarchy/resolver");
    final EntityID resolver = new EntityID("http://localhost:11111/naming/resolver");
    final EntityID trustAnchor = new EntityID("http://localhost:11111/naming/ta");
    final ResolverDifferentiator differentiator = new ResolverDifferentiator(
        RestClient.builder().build(),
        anarchyTrustAnchor,
        anarchyResolver,
        trustAnchor,
        resolver
    );

    final ResolverDifferentiator.ResponseDifference difference = differentiator.getResponseDifference(
        new ResolveRequest(
            "http://localhost:11111/im/im/op",
            trustAnchor.getValue(),
            null
        )
    );
    Assertions.assertNull(difference.getReference().getError());
    Assertions.assertNull(difference.getResponse().getError());
    Assertions.assertEquals(0, difference.getJsonDifference().size());

    final ResolverDifferentiator.ResponseDifference difference2 = differentiator.getResponseDifference(
        new ResolveRequest(
            "http://localhost:11111/im/im/rp",
            trustAnchor.getValue(),
            null
        )
    );
    Assertions.assertNull(difference2.getReference().getError());
    Assertions.assertNotNull(difference2.getResponse().getError());
    Assertions.assertEquals(400, difference2.getResponse().getError().getStatusCode());
  }
}
