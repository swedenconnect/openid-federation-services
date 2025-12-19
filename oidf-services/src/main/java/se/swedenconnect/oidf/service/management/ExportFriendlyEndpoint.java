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
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    final Map<String, String> icons = Map.of(
        "tmi", "pen",
        "ta", "anchor",
        "im", "globe",
        "openid_provider", "credit-card",
        "saml_identity_provider", "credit-card",
        "openid_relying_party", "user",
        "saml_service_provider", "user",
        "error", "exclamation-circle"
    );
    final Map<String, List<Map<String, Object>>> nodesAndEdges = this.exportEndpoint.getNodesAndEdges();
    final List<Map<String, String>> nodes = nodesAndEdges.get("nodes")
        .stream().map(node -> {
          final Boolean verified = (Boolean) node.get("verifiedSelfStatement");
          final Map<String, Object> claims = (Map<String, Object>) node.get("claims");
          final String sub = (String) claims.get("sub");
          final String iss = (String) claims.get("iss");

          final String evaluatedRole = Optional.ofNullable(claims.get("metadata"))
              .map(metadata -> (Map<String, Object>) metadata)
              .flatMap(metadata -> {
                return Optional.ofNullable(metadata.get("federation_entity"));
              })
              .map(metadata -> (Map<String, Object>) metadata)
              .map(federationMetadata -> {
                if (federationMetadata.containsKey("federation_resolve_endpoint")) {
                  return "ta";
                }
                if (federationMetadata.containsKey("federation_fetch_endpoint")) {
                  return "im";
                }
                if (federationMetadata.containsKey("federation_trust_mark_endpoint")) {
                  return "tmi";
                }
                return null;
              })
              .orElse(null);


          final List<String> types = ((Map<String, Object>) claims.get("metadata")).entrySet().stream()
              .filter(f -> !"federation_entity".equals(f.getKey()))
              .map(Map.Entry::getKey)
              .toList();
          final Map<String, Object> explanation = (Map<String, Object>) node.get("explanation");
          final boolean errorsPresent = Objects.nonNull(explanation) && !explanation.isEmpty();
          final String color =
              Map.of(true, "red", false, "green").get(errorsPresent);

          final Map<String, String> nodeJson = new HashMap<>(Map.of(
              "id", sub,
              "color", color,
              "title", sub,
              "icon", "check-circle"
          ));

          Optional.ofNullable(node.get("metrics"))
              .map(metrics -> (Map<String, String>) metrics)
              .ifPresent(metrics -> {
                nodeJson.put("arc__success", "" + metrics.get("success"));
                nodeJson.put("arc__failure", "" + metrics.get("failure"));
              });

          Optional.ofNullable(evaluatedRole).ifPresent(role -> {
            Optional.ofNullable(icons.get(role)).ifPresent(icon -> {
              nodeJson.put("icon", icon);
            });
          });

          try {
            final AtomicInteger counter = new AtomicInteger();
            JWKSet.parse((Map<String, Object>) claims.get("jwks")).getKeys()
                .stream()
                .map(JWK::getKeyID)
                .forEach(kid -> {
                  nodeJson.put("detail__kid_%s".formatted(counter.getAndIncrement()), kid);
                });
          } catch (final ParseException e) {
            nodeJson.put("detail__kid_failed_to_parse", "true");
          }

          Optional.ofNullable(types).ifPresent(foundTypes -> {
            if (!foundTypes.stream().filter(type -> !type.equals("federation_entity")).toList().isEmpty()) {
              final String type = foundTypes.getFirst();
              nodeJson.put("subtitle", type);
              Optional.ofNullable(icons.get(type)).ifPresent(icon -> {
                nodeJson.put("icon", icon);
              });
            }
          });

          if (errorsPresent) {
            final AtomicInteger counter = new AtomicInteger();
            explanation.forEach((a, b) -> {
              nodeJson.put("detail__expl_%s".formatted(counter.getAndIncrement()), b.toString());
            });
            nodeJson.put("icon", icons.get("error"));
          }


          return nodeJson;
        }).toList();
    final List<Map<String, String>> edges = nodesAndEdges.get("edges").stream().map(edge -> {
      final Map<String, Object> claims = (Map<String, Object>) edge.get("claims");
      final String sub = (String) claims.get("sub");
      final String iss = (String) claims.get("iss");
      final Map<String, String> edgeJson = new HashMap<>(Map.of(
          "id", iss + "|" + sub,
          "source", iss,
          "target", sub
      ));
      try {
        final AtomicInteger counter = new AtomicInteger();
        JWKSet.parse((Map<String, Object>) claims.get("jwks")).getKeys()
            .stream()
            .map(JWK::getKeyID)
            .forEach(kid -> {
              edgeJson.put("detail__kid_%s".formatted(counter.getAndIncrement()), kid);
            });
      } catch (final ParseException e) {
        edgeJson.put("detail__kid_failed_to_parse", "true");
      }
      return edgeJson;
    }).toList();

    return ExportEndpoint.MAPPER.writeValueAsString(Map.of("nodes", nodes, "edges", edges));
  }
}
