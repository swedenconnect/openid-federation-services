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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.JWKSet;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.service.resolver.ResolverFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheRegistry;
import se.swedenconnect.oidf.service.state.StateHashFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Export endpoint for oidf service.
 *
 * @author Felix Hellman
 */
@Endpoint(id = "export")
@Component
@Slf4j
@AllArgsConstructor
public class ExportEndpoint {
  private final ResolverCacheRegistry registry;
  private final CompositeRecordSource source;
  private final ResolverFactory factory;
  private final MeterRegistry meterRegistry;
  /**
   * JSON mapper for serializing graphs
   */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    MAPPER.registerModule(new JavaTimeModule());

    final SimpleModule module = new SimpleModule();
    module.addSerializer(JWKSet.class, new StateHashFactory.JWKSetSerializer());
    module.addDeserializer(JWKSet.class, new StateHashFactory.JWKSetDeserializer());
    MAPPER.registerModule(module);
  }

  /**
   * Exports federation as json
   * @return federation as json
   * @throws JsonProcessingException
   */
  @ReadOperation
  public String exportFederation() throws JsonProcessingException {
    final Map<String, List<Map<String, Object>>> nodes = this.getNodesAndEdges();
    return MAPPER.writeValueAsString(nodes);
  }

  /**
   * Gets nodes an edges in map format
   * @return map of nodes and edges
   */
  public Map<String, List<Map<String, Object>>> getNodesAndEdges() {
    final ResolverProperties properties = this.source.getResolverProperties().getFirst();
    final List<ExportStatement> selfStatements = new ArrayList<>();
    final List<ExportStatement> subordinateStatements = new ArrayList<>();

    final EntityStatementTree tree = this.registry.getRegistration(properties.entityIdentifier()).get().tree();
    tree.getAll()
        .stream()
        .map(Tree.SearchResult::getData)
        .peek(es -> es.getClaimsSet().toJSONObject())
        .forEach(es -> {
          if (es.getClaimsSet().isSelfStatement()) {
            selfStatements.add(new ExportStatement(es));
          } else {
            subordinateStatements.add(new ExportStatement(es));
          }
        });

    selfStatements.forEach(ss -> {
      try {
        final Map<Integer, Map<String, String>> explain = this.factory.create(properties).explain(new ResolveRequest(
            ss.getEntityStatement().getEntityID().getValue(), properties.trustAnchor(), null, true));
        ss.withResolverExplanation(explain);
        final double success = this.meterRegistry.counter("GET_entity_configuration", List.of(
            Tag.of("entityId", ss.getEntityStatement().getEntityID().getValue()),
            Tag.of("outcome", "success")
        )).count();
        final double failure = this.meterRegistry.counter("GET_entity_configuration", List.of(
            Tag.of("entityId", ss.getEntityStatement().getEntityID().getValue()),
            Tag.of("outcome", "failure")
        )).count();
        final double total = success + failure;
        ss.withMetrics(total, success, failure);
      } catch (final Exception e) {
        log.error("Failed to add explanation to entity statement from resolver", e);
      }
    });

    final Map<String, List<Map<String, Object>>> nodes = Map.of(
        "nodes", selfStatements.stream().map(ExportStatement::toJsonObject).toList(),
        "edges", subordinateStatements.stream().map(ExportStatement::toJsonObject).toList()
    );
    return nodes;
  }
}
