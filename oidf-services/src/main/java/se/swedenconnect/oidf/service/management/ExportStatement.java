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
package se.swedenconnect.oidf.service.management;

import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Data class to transform entity statement into something that can be easily presented in a graph.
 *
 * @author Felix Hellman
 */
public class ExportStatement {

  /**
   * Constructor.
   * @param entityStatement
   */
  public ExportStatement(final SignedJWT entityStatement) {
    this.entityStatement = entityStatement;
  }

  @Getter
  private final SignedJWT entityStatement;
  private Map<Integer, Map<String, String>> explanation;
  private Double success;
  private Double failure;
  private Double total;
  private Integer successCount;
  private Integer failureCount;
  private Double uptimeWindow;
  private String lastSeenReachable;
  private Long avgResponseMs;

  /**
   * @return json object
   */
  public Map<String, Object> toJsonObject() {
    final HashMap<String, Object> json = new HashMap<>();
    json.put("entityId", EntityStatementClaims.getEntityID(this.entityStatement).getValue());
    json.put("claims", EntityStatementClaims.claims(this.entityStatement).toJSONObject());
    if (EntityStatementClaims.isSelfStatement(this.entityStatement)) {
      try {
        EntityStatementClaims.verifySignatureOfSelfStatement(this.entityStatement);
        json.put("verifiedSelfStatement", true);
      } catch (final Exception e) {
        json.put("verifiedSelfStatement", false);
      }
    }
    Optional.ofNullable(this.explanation).ifPresent(expl -> {
      json.put("explanation", expl);
    });

    Optional.ofNullable(this.total).ifPresent(tot -> {
      json.put("metrics", Map.of("total", tot, "success", this.success, "failure", this.failure));
      json.put("mainstat", this.successCount);
      json.put("secondarystat", this.failureCount);
    });

    Optional.ofNullable(this.uptimeWindow).ifPresent(w -> {
      json.put("uptime", Map.of(
          "ratio", w,
          "lastSeen", this.lastSeenReachable != null ? this.lastSeenReachable : "never",
          "avgResponseMs", this.avgResponseMs != null ? this.avgResponseMs : 0L
      ));
    });

    return json;
  }

  /**
   * Add resolver explanation to this node
   * @param explanation
   * @return this
   */
  public ExportStatement withResolverExplanation(final Map<Integer, Map<String, String>> explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Add sliding-window uptime data to this node.
   *
   * @param uptimeWindow    ratio of successful fetches in the last N cycles (0.0–1.0)
   * @param lastSeenReachable ISO-8601 timestamp of last successful fetch, or null
   * @param avgResponseMs   exponential moving average response time in ms, or null
   * @return this
   */
  public ExportStatement withUptimeData(
      final double uptimeWindow,
      final String lastSeenReachable,
      final Long avgResponseMs) {
    this.uptimeWindow = uptimeWindow;
    this.lastSeenReachable = lastSeenReachable;
    this.avgResponseMs = avgResponseMs;
    return this;
  }

  /**
   * Add metrics to this node
   * @param total
   * @param success
   * @param failure
   * @return this
   */
  public ExportStatement withMetrics(final double total, final double success, final double failure) {
    if (total != 0) {
      this.success = success / total;
      this.failure = failure / total;
      this.successCount = (int) Math.floor(this.success);
      this.failureCount = (int) Math.floor(this.failure);
      this.total = total;
    }
    return this;
  }
}
