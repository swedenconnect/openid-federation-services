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
package se.swedenconnect.oidf.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.suites.Context;

@ActiveProfiles({"integration-test"})
public class GeneralErrorHandlingTestCases {

  private RestClient restClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
    this.restClient = RestClient.builder()
        .baseUrl("http://localhost:" + Context.getServicePort())
        .build();
  }

  @Test
  public void testBrowserRequestExpectNotFound() throws Exception {
    final ResponseEntity<String> response = this.restClient.get()
        .uri("/notfound/myfrend")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,"
            + "image/apng,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Connection", "keep-alive")
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {})
        .toEntity(String.class);

    Assertions.assertEquals(404, response.getStatusCode().value());
    final JsonNode body = this.objectMapper.readTree(response.getBody());
    Assertions.assertEquals("not_found", body.get("error").asText());
    Assertions.assertEquals("No static resource notfound/myfrend.", body.get("error_description").asText());
    Assertions.assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  public void testMethodNotSupported() throws Exception {
    final ResponseEntity<String> response = this.restClient.delete()
        .uri("/authorization-tmi/trust_mark")
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {})
        .toEntity(String.class);

    Assertions.assertEquals(404, response.getStatusCode().value());
    final JsonNode body = this.objectMapper.readTree(response.getBody());
    Assertions.assertEquals("not_found", body.get("error").asText());
    Assertions.assertEquals("No static resource authorization-tmi/trust_mark.", body.get("error_description").asText());
    Assertions.assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  public void testMissingParamExpect400() throws Exception {
    final ResponseEntity<String> response = this.restClient.get()
        .uri("/im/tmi/trust_mark_listing")
        .header("Content-Type", "VerySpecialContentType")
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {})
        .toEntity(String.class);

    Assertions.assertEquals(400, response.getStatusCode().value());
    final JsonNode body = this.objectMapper.readTree(response.getBody());
    Assertions.assertEquals("invalid_request", body.get("error").asText());
    Assertions.assertEquals("Required request parameter [trust_mark_type] was missing.",
        body.get("error_description").asText());
    Assertions.assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON));
  }

}
