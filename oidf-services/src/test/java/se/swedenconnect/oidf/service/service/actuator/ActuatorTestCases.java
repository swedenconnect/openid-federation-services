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
package se.swedenconnect.oidf.service.service.actuator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.suites.Context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActuatorTestCases {

  @BeforeEach
  public void beforeMethod() {
    final ThreadLocal<ApplicationContext> applicationContext = Context.applicationContext;
    final boolean context = applicationContext != null;
    org.junit.Assume.assumeTrue(context);
  }

  @Test
  void testPrometheusMetrics() throws InterruptedException {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(Context.getManagementPort())).build();
    final String body = client.get().uri("/actuator/prometheus")
        .retrieve()
        .body(String.class);
    System.out.println(body);
  }

  @Test
  void testGrafanaExport() throws Exception {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(Context.getManagementPort())).build();
    final String body = client.get().uri("/actuator/export-grafana")
        .retrieve()
        .body(String.class);
    assertNotNull(body);
    final JsonNode json = new ObjectMapper().readTree(body);
    assertTrue(json.has("nodes"), "Response should contain 'nodes' key");
    assertTrue(json.has("edges"), "Response should contain 'edges' key");
    assertTrue(json.get("nodes").isArray(), "'nodes' should be an array");
    assertTrue(json.get("edges").isArray(), "'edges' should be an array");
    assertFalse(json.get("nodes").isEmpty(), "'nodes' should not be empty");
    assertFalse(json.get("edges").isEmpty(), "'edges' should not be empty");
  }

  @Test
  void testGrafanaExportAnarchy() throws Exception {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(Context.getManagementPort())).build();
    final String body = client.get()
        .uri("/actuator/export-grafana?trustAnchor=http://localhost:11111/anarchy/ta")
        .retrieve()
        .body(String.class);
    assertNotNull(body);
    final JsonNode json = new ObjectMapper().readTree(body);
    assertTrue(json.has("nodes"), "Response should contain 'nodes' key");
    assertTrue(json.has("edges"), "Response should contain 'edges' key");
    assertTrue(json.get("nodes").isArray(), "'nodes' should be an array");
    assertTrue(json.get("edges").isArray(), "'edges' should be an array");
    assertFalse(json.get("nodes").isEmpty(), "'nodes' should not be empty");
    assertFalse(json.get("edges").isEmpty(), "'edges' should not be empty");
  }
}
