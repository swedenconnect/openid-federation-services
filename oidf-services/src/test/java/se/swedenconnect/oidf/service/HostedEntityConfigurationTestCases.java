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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.suites.Context;

import java.net.URI;

public class HostedEntityConfigurationTestCases {

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

  @Test
  @DisplayName("GET <ie>/hosted/<URL-encoded id>/.well-known/openid-federation : 200 returns entity configuration")
  void testHostedEntityConfigurationWithEncodedEntityId() throws Exception {
    final URI uri = URI.create(("http://localhost:%d/im/hosted/http%%3A%%2F%%2Flocalhost%%3A11111%%2Fhosted-rp"
        + "/.well-known/openid-federation").formatted(Context.getServicePort()));
    final ResponseEntity<String> response = RestClient.create().get().uri(uri).retrieve().toEntity(String.class);
    Assertions.assertEquals(200, response.getStatusCode().value());
    Assertions.assertNotNull(response.getHeaders().getContentType());
    Assertions.assertTrue(MediaType.parseMediaType("application/entity-statement+jwt")
        .isCompatibleWith(response.getHeaders().getContentType()));
    Assertions.assertNotNull(response.getBody());
    final SignedJWT jwt = SignedJWT.parse(response.getBody());
    Assertions.assertNotNull(jwt.getJWTClaimsSet().getSubject());
  }
}
