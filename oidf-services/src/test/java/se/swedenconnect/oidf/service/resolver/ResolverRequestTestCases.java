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

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.text.ParseException;

/**
 * Tests for the parameters of the resolve request.
 */
public class ResolverRequestTestCases {

  private static final String RESOLVER = "http://localhost:11111/anarchy/resolver";
  private static final String TRUST_ANCHOR = "http://localhost:11111/anarchy/ta";
  private static final String SUBJECT = "http://localhost:11111/im/op";

  @Test
  void anyConfiguredTrustAnchorAmongSeveralIsUsed() throws ParseException {
    final ResolverDifferentiator.Response response =
        resolve("http://localhost:11111/unknown/ta", TRUST_ANCHOR);

    Assertions.assertNull(response.getError());
    Assertions.assertEquals(SUBJECT, SignedJWT.parse(response.getBody()).getJWTClaimsSet().getSubject());
  }

  @Test
  void noConfiguredTrustAnchorIsRejected() {
    final ResolverDifferentiator.Response response =
        resolve("http://localhost:11111/unknown/ta", "http://localhost:11111/other/ta");

    Assertions.assertNotNull(response.getError());
    Assertions.assertEquals(404, response.getError().getStatusCode());
    Assertions.assertTrue(response.getError().getMessage().contains("invalid_trust_anchor"));
  }

  private static ResolverDifferentiator.Response resolve(final String... trustAnchors) {
    return RestClient.builder().baseUrl(RESOLVER).build()
        .get()
        .uri(uri -> uri.path("/resolve")
            .queryParam("sub", SUBJECT)
            .queryParam("trust_anchor", (Object[]) trustAnchors)
            .build())
        .exchange((req, res) -> {
          final String body = new String(res.getBody().readAllBytes());
          if (res.getStatusCode().isError()) {
            return ResolverDifferentiator.Response.builder()
                .error(ResolverDifferentiator.Response.Error.builder()
                    .statusCode(res.getStatusCode().value())
                    .message(body)
                    .build())
                .build();
          }
          return ResolverDifferentiator.Response.builder().body(body).build();
        });
  }
}
