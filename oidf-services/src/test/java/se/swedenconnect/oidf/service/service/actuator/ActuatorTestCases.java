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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.service.suites.Context;

import java.util.List;
import java.util.Set;

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
    assertValidNodeGraphData(json);
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
    assertValidNodeGraphData(json);
  }

  /**
   * Asserts that the Grafana Node Graph panel can render the exported data without crashing.
   * <p>
   * Grafana's Node Graph panel accesses panel options (including {@code nodeRadius}) during
   * rendering. When arc/stat fields are serialized as JSON strings instead of numbers the panel
   * fails to build its layout config, resulting in:
   * {@code TypeError: Cannot read properties of undefined (reading 'nodeRadius')}
   * <p>
   * Required node fields: {@code id} (string).
   * Required edge fields: {@code id}, {@code source}, {@code target} (strings).
   * Arc and stat fields must be JSON numbers, not strings.
   */
  private static void assertValidNodeGraphData(final JsonNode json) {
    final Set<String> arcFields = Set.of("arc__success", "arc__failure", "arc__validation");
    final Set<String> statFields = Set.of("mainstat", "seconddarystat");

    json.get("nodes").forEach(node -> {
      assertTrue(node.has("id") && node.get("id").isTextual() && !node.get("id").asText().isBlank(),
          "Each node must have a non-blank string 'id' — got: " + node);
      assertTrue(node.has("nodeRadius") && node.get("nodeRadius").isNumber(),
          "Each node must have a numeric 'nodeRadius' — missing field causes: " +
          "TypeError: Cannot read properties of undefined (reading 'nodeRadius'). Got: " + node);

      arcFields.forEach(field -> {
        if (node.has(field)) {
          assertTrue(node.get(field).isNumber(),
              "Node arc field '" + field + "' must be a JSON number, not a string — " +
              "string arc values prevent Grafana's Node Graph panel from initializing nodeRadius. " +
              "Got: " + node.get(field));
        }
      });

      statFields.forEach(field -> {
        if (node.has(field)) {
          assertTrue(node.get(field).isNumber(),
              "Node stat field '" + field + "' must be a JSON number, not a string — " +
              "string stat values prevent Grafana's Node Graph panel from initializing nodeRadius. " +
              "Got: " + node.get(field));
        }
      });
    });

    final Set<String> nodeIds = new java.util.HashSet<>();
    json.get("nodes").forEach(node -> nodeIds.add(node.get("id").asText()));

    json.get("edges").forEach(edge -> {
      assertTrue(edge.has("id") && edge.get("id").isTextual() && !edge.get("id").asText().isBlank(),
          "Each edge must have a non-blank string 'id' — got: " + edge);
      assertTrue(edge.has("source") && edge.get("source").isTextual() && !edge.get("source").asText().isBlank(),
          "Each edge must have a non-blank string 'source' — got: " + edge);
      assertTrue(edge.has("target") && edge.get("target").isTextual() && !edge.get("target").asText().isBlank(),
          "Each edge must have a non-blank string 'target' — got: " + edge);

      final String source = edge.get("source").asText();
      assertTrue(nodeIds.contains(source),
          "Edge source '" + source + "' has no matching node id — edge: " + edge);

      final String target = edge.get("target").asText();
      assertTrue(nodeIds.contains(target),
          "Edge target '" + target + "' has no matching node id — edge: " + edge);
    });
  }

  @Test
  void testDeadNodesContainsBrokenSubordinate() throws Exception {
    final RestClient client = RestClient.builder().baseUrl("http://localhost:%d".formatted(Context.getManagementPort())).build();
    final String body = client.get()
        .uri("/actuator/dead-nodes?trustAnchor=http://localhost:11111/anarchy/ta")
        .retrieve()
        .body(String.class);
    assertNotNull(body);
    final List<String> deadNodes = new ObjectMapper().readValue(body, new TypeReference<>() {});
    assertTrue(deadNodes.contains("http://localhost:11112/broken/subordinate"),
        "Dead nodes should contain the broken subordinate");
  }
}
