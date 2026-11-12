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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.UptimeRegistry;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementWrapper;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.resolver.ResolverCacheRegistry;
import se.swedenconnect.oidf.resolver.ResolverFactory;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import tools.jackson.databind.json.JsonMapper;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Actuator endpoint that exports the federation tree of a resolver as a JSON graph of nodes and edges.
 * <p>
 * Nodes are the self-signed entity configurations, enriched with the resolver explanation, request metrics, uptime
 * data, the time the entity was last seen, the latest fetch error and the status of its trust marks. Edges are the
 * subordinate statements.
 * </p>
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Endpoint(id = "export")
@Component
@Slf4j
public class ExportEndpoint {

  private final @NonNull ResolverCacheRegistry registry;
  private final @NonNull CompositeRecordSource source;
  private final @NonNull ResolverFactory factory;
  private final @NonNull MeterRegistry meterRegistry;
  private final @NonNull UptimeRegistry uptimeRegistry;

  /** JSON mapper for the export, leaving out null and empty values. */
  private final @NonNull JsonMapper mapper;

  /**
   * Constructor.
   *
   * @param registry the resolver cache registry holding the resolve trees
   * @param source the record source used to find resolver configuration
   * @param factory the resolver factory, used to explain resolve results
   * @param meterRegistry the meter registry holding entity configuration request metrics
   * @param uptimeRegistry the uptime registry holding scraping results
   * @param jsonMapper the application JSON mapper, used as the base for the export mapper
   */
  public ExportEndpoint(
      final @NonNull ResolverCacheRegistry registry,
      final @NonNull CompositeRecordSource source,
      final @NonNull ResolverFactory factory,
      final @NonNull MeterRegistry meterRegistry,
      final @NonNull UptimeRegistry uptimeRegistry,
      final @NonNull JsonMapper jsonMapper) {
    this.registry = registry;
    this.source = source;
    this.factory = factory;
    this.meterRegistry = meterRegistry;
    this.uptimeRegistry = uptimeRegistry;
    this.mapper = jsonMapper.rebuild()
        .changeDefaultPropertyInclusion(incl -> incl
            .withValueInclusion(JsonInclude.Include.NON_EMPTY)
            .withContentInclusion(JsonInclude.Include.NON_EMPTY))
        .build();
  }

  /**
   * Exports the federation as JSON.
   *
   * @param trustAnchor optional trust anchor entity identifier selecting the resolver, defaults to the first
   *     configured resolver
   * @return the federation as a JSON object with the members {@code nodes} and {@code edges}
   * @throws IllegalArgumentException if no resolver is configured for the given trust anchor
   */
  @ReadOperation
  public @NonNull String exportFederation(final @Nullable String trustAnchor) {
    return this.toJson(this.getNodesAndEdges(trustAnchor));
  }

  /**
   * Serializes an object to JSON using the export mapper, where null and empty values are left out.
   *
   * @param value the object to serialize
   * @return JSON string
   */
  public @NonNull String toJson(final @NonNull Object value) {
    return this.mapper.writeValueAsString(value);
  }

  /**
   * Resolves the effective trust anchor.
   *
   * @param trustAnchor optional trust anchor entity identifier, defaults to the trust anchor of the first
   *     configured resolver
   * @return the trust anchor entity identifier
   * @throws IllegalArgumentException if no resolver is configured for the given trust anchor
   */
  public @NonNull String resolveEffectiveTrustAnchor(final @Nullable String trustAnchor) {
    return this.getResolverProperties(trustAnchor).getTrustAnchor();
  }

  /**
   * Returns the nodes and edges of the federation tree for the given trust anchor.
   *
   * @param trustAnchor optional trust anchor entity identifier selecting the resolver, defaults to the first
   *     configured resolver
   * @return map with the lists {@code nodes} (entity configurations) and {@code edges} (subordinate statements)
   * @throws IllegalArgumentException if no resolver is configured for the given trust anchor
   */
  public @NonNull Map<String, List<Map<String, Object>>> getNodesAndEdges(final @Nullable String trustAnchor) {
    final ResolverProperties properties = this.getResolverProperties(trustAnchor);
    final List<ScrapedEntity> selfEntities = new ArrayList<>();
    final List<ExportStatement> subordinateStatements = new ArrayList<>();

    final EntityStatementTree tree = this.registry.getRegistration(properties.getEntityIdentifier())
        .orElseThrow(() -> new IllegalStateException(
            "No resolver tree registered for " + properties.getEntityIdentifier()))
        .tree();
    tree.getAll()
        .stream()
        .map(Tree.SearchResult::getData)
        .filter(Objects::nonNull)
        .forEach(entity -> {
          if (Objects.nonNull(entity.getIntermediate())) {
            entity.getIntermediate().subordinates().values().forEach(jwt ->
                subordinateStatements.add(new ExportStatement(jwt)));
          }
          if (EntityStatementClaims.isSelfStatement(entity.getEntityStatement())) {
            selfEntities.add(entity);
          }
          else {
            subordinateStatements.add(new ExportStatement(entity.getEntityStatement()));
          }
        });

    final List<ExportStatement> selfStatements = selfEntities.stream()
        .map(entity -> {
          final ExportStatement statement = new ExportStatement(entity.getEntityStatement());
          this.addNodeDetails(statement, entity, properties);
          return statement;
        })
        .toList();

    return Map.of(
        "nodes", selfStatements.stream().map(ExportStatement::toJsonObject).toList(),
        "edges", subordinateStatements.stream().map(ExportStatement::toJsonObject).toList()
    );
  }

  /**
   * Finds the resolver configuration for a trust anchor.
   *
   * @param trustAnchor optional trust anchor entity identifier, null gives the first configured resolver
   * @return the resolver configuration
   * @throws IllegalArgumentException if no resolver is configured for the given trust anchor
   */
  private @NonNull ResolverProperties getResolverProperties(final @Nullable String trustAnchor) {
    if (trustAnchor == null) {
      return this.source.getResolverProperties().getFirst();
    }
    return this.source.getResolverProperties().stream()
        .filter(p -> trustAnchor.equals(p.getTrustAnchor()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No resolver found for trust anchor: " + trustAnchor));
  }

  /**
   * Adds the resolver explanation, request metrics, uptime data and scrape data to a node. A failure is logged and
   * leaves the node without the remaining details.
   *
   * @param statement the self-signed entity configuration
   * @param entity the entity in the resolver tree
   * @param properties the resolver configuration
   */
  private void addNodeDetails(final @NonNull ExportStatement statement, final @NonNull ScrapedEntity entity,
      final @NonNull ResolverProperties properties) {
    try {
      final String entityId = EntityStatementClaims.getEntityID(statement.getEntityStatement()).getValue();
      final Instant newestSeen = newer(entity.getLastSeen(),
          this.uptimeRegistry.getLastSeenReachable(entityId).orElse(null));
      statement.withScrapeData(
          Optional.ofNullable(newestSeen).map(Instant::toString).orElse(null),
          Optional.ofNullable(entity.getLastError()).map(Object::toString).orElse(null),
          trustMarks(entity));

      final Map<Integer, Map<String, String>> explain = this.factory.create(properties)
          .explain(new ResolveRequest(entityId, properties.getTrustAnchor(), null, true));
      if (explain != null) {
        statement.withResolverExplanation(explain);
      }

      final double success = this.meterRegistry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", entityId),
          Tag.of("outcome", "success")
      )).count();
      final double failure = this.meterRegistry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", entityId),
          Tag.of("outcome", "failure")
      )).count();
      statement.withMetrics(success + failure, success, failure);

      this.uptimeRegistry.uptimeRatio(entityId).ifPresent(ratio -> {
        final Long avgMs = this.uptimeRegistry.getAvgResponseMs(entityId).orElse(null);
        statement.withUptimeData(ratio, avgMs);
      });
    }
    catch (final Exception e) {
      log.warn("Failed to add resolver explanation and metrics to export node", e);
    }
  }

  /**
   * Gives the newer of two times.
   *
   * @param a a time, or null
   * @param b a time, or null
   * @return the newer time, the other one if one is null, or null if both are
   */
  private static @Nullable Instant newer(final @Nullable Instant a, final @Nullable Instant b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isAfter(b) ? a : b;
  }

  /**
   * Lists the trust marks of an entity, each with its type, the status from the stored status response and the time
   * the status was last checked.
   *
   * @param entity the entity
   * @return list of trust marks
   */
  private static @NonNull List<Map<String, Object>> trustMarks(final @NonNull ScrapedEntity entity) {
    final Map<String, TrustMarkStatusResponse> statuses =
        Optional.ofNullable(entity.getTrustMarkStatuses()).orElseGet(Map::of);
    return new EntityStatementWrapper(entity.getEntityStatement()).getTrustMarks().stream()
        .map(trustMark -> {
          final Map<String, Object> json = new HashMap<>();
          json.put("type", EntityStatementClaims.getTrustMarkType(trustMark));
          Optional.ofNullable(statuses.get(trustMark.serialize())).ifPresent(status -> {
            json.put("status", status(status));
            json.put("last_checked",
                Optional.ofNullable(status.getLastChecked()).map(Instant::toString).orElse(null));
          });
          return json;
        })
        .toList();
  }

  /**
   * Reads the {@code status} claim of a stored trust mark status response.
   *
   * @param response the stored response
   * @return the status, or null if there is no status response or it cannot be parsed
   */
  private static @Nullable String status(final @NonNull TrustMarkStatusResponse response) {
    if (response.getSignedJWT() == null) {
      return null;
    }
    try {
      return response.getSignedJWT().getJWTClaimsSet().getStringClaim("status");
    }
    catch (final ParseException e) {
      return null;
    }
  }
}
