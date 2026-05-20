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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.ModuleResponseCache;
import se.swedenconnect.oidf.common.entity.tree.scraping.CacheSnapshotVersionLookup;
import se.swedenconnect.oidf.resolver.TreeUpdatedEvent;

import java.util.List;
import java.util.Map;

/**
 * Pre-warms the Grafana export cache after each resolver tree reload.
 * Ensures a cached value is available for every configured trust anchor
 * without waiting for the first inbound request.
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
@AllArgsConstructor
public class GrafanaExportCacheWarmer {

  private final ExportEndpoint exportEndpoint;
  private final ModuleResponseCache cache;
  private final CacheSnapshotVersionLookup lookup;
  private final CompositeRecordSource source;

  /**
   * Handles tree updated events by pre-warming the Grafana export cache.
   *
   * @param event the tree updated event
   */
  @EventListener
  public void handle(final TreeUpdatedEvent event) {
    this.source.getResolverProperties().stream()
        .filter(p -> p.getEntityIdentifier().equals(event.entityId()))
        .findFirst()
        .ifPresent(properties -> {
          final String trustAnchor = properties.getTrustAnchor();
          try {
            log.debug("Pre-warming Grafana export cache for trust anchor '{}'", trustAnchor);
            final Map<String, List<Map<String, Object>>> nodesAndEdges =
                this.exportEndpoint.getNodesAndEdges(trustAnchor);
            final String result = ExportFriendlyEndpoint.formatAsGrafana(nodesAndEdges);
            final long snapshot = this.lookup.getLatestSnapshotVersion();
            this.cache.put(snapshot, ExportFriendlyEndpoint.cacheKey(trustAnchor),
                new CachedResponse(result, "application/json", 200));
            log.debug("Grafana export cache warmed for trust anchor '{}'", trustAnchor);
          } catch (final Exception e) {
            log.warn("Failed to pre-warm Grafana export cache for trust anchor '{}'", trustAnchor, e);
          }
        });
  }
}
