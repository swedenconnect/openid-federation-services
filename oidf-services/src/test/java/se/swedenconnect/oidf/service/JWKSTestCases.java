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
package se.swedenconnect.oidf.service;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.suites.Context;

public class JWKSTestCases {

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

  @Test
  @DisplayName("GET /jwks : 200 returns signed JWT with federation, hosted, and name claims")
  void testJwksEndpointReturnsParsableSignedJwt() throws Exception {
    final RestClient client = RestClient.builder()
        .baseUrl("http://localhost:%d".formatted(Context.getServicePort()))
        .build();
    final String body = client.get().uri("/jwks").retrieve().body(String.class);
    Assertions.assertNotNull(body);
    final SignedJWT jwt = SignedJWT.parse(body);
    Assertions.assertNotNull(jwt.getJWTClaimsSet().getClaim("federation"), "federation claim missing");
    Assertions.assertNotNull(jwt.getJWTClaimsSet().getClaim("hosted"), "hosted claim missing");
    Assertions.assertNotNull(jwt.getJWTClaimsSet().getClaim("name"), "name claim missing");
  }
}
