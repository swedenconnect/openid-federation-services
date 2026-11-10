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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
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
import java.util.List;
import java.util.Map;

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

  @Test
  void nullValueOperatorIsPublishedInSubordinateStatement(final FederationClients clients) throws ParseException {
    final SignedJWT statement = clients.policyTrustAnchor().fetch(TestFederationEntities.IM.INTERMEDIATE);

    final Map<String, Object> policy = statement.getJWTClaimsSet().getJSONObjectClaim("metadata_policy");
    final Map<String, Object> logoUri = JSONObjectUtils.getJSONObject(
        JSONObjectUtils.getJSONObject(policy, "openid_provider"), "logo_uri");
    Assertions.assertTrue(logoUri.containsKey("value"), "metadata_policy was " + policy);
    Assertions.assertNull(logoUri.get("value"));
    Assertions.assertEquals(Map.of("subset_of", List.of("RS256")), JSONObjectUtils.getJSONObject(
        policy, "openid_provider").get("id_token_signing_alg_values_supported"));
  }

  @Test
  void nullValueOperatorRemovesParameterWhenResolving(final FederationClients clients) throws ParseException {
    final SignedJWT response = clients.policyResolver()
        .resolve(TestFederationEntities.IM.OP, TestFederationEntities.Policy.TRUST_ANCHOR, null);

    final Map<String, Object> op = JSONObjectUtils.getJSONObject(
        response.getJWTClaimsSet().getJSONObjectClaim("metadata"), "openid_provider");
    Assertions.assertNotNull(op);
    Assertions.assertFalse(op.containsKey("logo_uri"), "openid_provider metadata was " + op);
  }
}
