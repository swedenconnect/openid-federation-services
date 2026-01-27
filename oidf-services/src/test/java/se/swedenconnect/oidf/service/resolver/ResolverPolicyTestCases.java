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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.MapDifference;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.service.entity.TestFederationEntities;
import se.swedenconnect.oidf.service.service.testclient.FederationClients;
import se.swedenconnect.oidf.service.service.testclient.TestFederationClientParameterResolver;

import java.text.ParseException;

@Slf4j
@ExtendWith(TestFederationClientParameterResolver.class)
public class ResolverPolicyTestCases {
  @Test
  void testPolicyFiltering(final FederationClients clients) throws ParseException {
    final ResolverDifferentiator.ResponseDifference difference = clients.policy().getResponseDifference(new ResolveRequest(
        TestFederationEntities.IM.OP.getValue(),
        TestFederationEntities.Policy.TRUST_ANCHOR.getValue(),
        null, false
    ));
    final MapDifference.ValueDifference<Object> metadata = difference.getJsonDifference().get("metadata");
    //Verify difference in metadata
    Assertions.assertNotNull(metadata);
  }
}
