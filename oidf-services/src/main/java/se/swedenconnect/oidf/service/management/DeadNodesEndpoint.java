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

import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.ResolverCacheRegistry;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.List;
import java.util.Objects;

/**
 * Actuator endpoint that lists dead nodes in the federation tree.
 * A dead node is a subordinate that has been listed by an intermediate but whose
 * entity configuration could not be fetched (e.g. the host is unreachable).
 *
 * @author Felix Hellman
 */
@Endpoint(id = "dead-nodes")
@Component
@AllArgsConstructor
public class DeadNodesEndpoint {

  private final ResolverCacheRegistry registry;
  private final CompositeRecordSource source;

  /**
   * Returns the entity identifiers of all dead nodes for the given trust anchor.
   *
   * @param trustAnchor optional trust anchor entity identifier, defaults to first available
   * @return list of entity identifiers that could not be scraped
   */
  @ReadOperation
  public List<String> getDeadNodes(@Nullable final String trustAnchor) {
    final ResolverProperties properties = trustAnchor == null
        ? this.source.getResolverProperties().getFirst()
        : this.source.getResolverProperties().stream()
            .filter(p -> trustAnchor.equals(p.getTrustAnchor()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No resolver found for trust anchor: " + trustAnchor));

    final EntityStatementTree tree = this.registry.getRegistration(properties.getEntityIdentifier()).get().tree();
    return tree.getAll()
        .stream()
        .filter(result -> Objects.isNull(result.getData()))
        .map(result -> result.node().getKey().entityId())
        .toList();
  }
}
