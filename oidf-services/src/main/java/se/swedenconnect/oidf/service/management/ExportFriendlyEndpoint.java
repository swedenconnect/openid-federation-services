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
package se.swedenconnect.oidf.service.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Grafana friendly export of JSON graph
 *
 * https://grafana.com/docs/grafana/latest/visualizations/panels-visualizations/visualizations/node-graph
 * https://developers.grafana.com/ui/latest/index.html?path=/story/iconography-icon--icons-overview
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
@Endpoint(id = "export-grafana")
@Component
public class ExportFriendlyEndpoint {
  private final ExportEndpoint exportEndpoint;

  /**
   * Exports federation in grafana friendly format
   * @return json string
   * @throws JsonProcessingException
   */
  @ReadOperation
  public String getGrafanaFriendlyJson() throws JsonProcessingException {
    final Map<String, List<Map<String, Object>>> nodesAndEdges = this.exportEndpoint.getNodesAndEdges();
    final List<Map<String, String>> nodes = nodesAndEdges.get("nodes")
        .stream().map(node -> {
          final Boolean verified = (Boolean) node.get("verifiedSelfStatement");
          final Map<String, Object> claims = (Map<String, Object>) node.get("claims");
          final String sub = (String) claims.get("sub");
          final String iss = (String) claims.get("iss");
          final List<String> types = ((Map<String, Object>) claims.get("metadata")).entrySet().stream()
              .map(Map.Entry::getKey)
              .toList();
          final Map<String, Object> explanation = (Map<String, Object>) node.get("explanation");
          final String color =
              Map.of(true, "red", false, "green").get(Objects.nonNull(explanation) && !explanation.isEmpty());

          final Map<String, String> nodeJson = new HashMap<>(Map.of(
              "id", sub,
              "color", color,
              "title", sub,
              "icon", "check-circle"
          ));

          Optional.ofNullable(types).ifPresent(foundTypes -> {
            if (!foundTypes.stream().filter(type -> !type.equals("federation_entity")).toList().isEmpty()) {
              nodeJson.put("subtitle", foundTypes.getFirst());
            }
          });

          return nodeJson;
        }).toList();
    final List<Map<String, String>> edges = nodesAndEdges.get("edges").stream().map(edge -> {
      final Map<String, Object> claims = (Map<String, Object>) edge.get("claims");
      final String sub = (String) claims.get("sub");
      final String iss = (String) claims.get("iss");
      return Map.of(
          "id", iss + "|" + sub,
          "source", iss,
          "target", sub
      );
    }).toList();

    return ExportEndpoint.MAPPER.writeValueAsString(Map.of("nodes", nodes, "edges", edges));
  }
}
