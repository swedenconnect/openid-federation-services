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
package se.swedenconnect.oidf.resolver.tree.resolution;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Deduplicates entity scraping operations within a single tree-loading iteration.
 *
 * @author Felix Hellman
 */
public class LoaderContext {
  private final Map<EntityID, CompletableFuture<ScrapedEntity>> scrapedEntities = new ConcurrentHashMap<>();

  /**
   * Returns the scraped entity for the given ID, loading it via the client if not already in progress.
   *
   * @param entityID the entity to load
   * @param client   the federation client used to scrape the entity
   * @return the scraped entity
   */
  public ScrapedEntity getOrLoad(final EntityID entityID, final FederationClient client) {
    try {
      return this.scrapedEntities.computeIfAbsent(entityID, key -> {
        return CompletableFuture.supplyAsync(() -> {
          final ScrapedEntity scrapedEntity = ScrapedEntity.builder().entityID(key).build();
          scrapedEntity.scrape(client);
          return scrapedEntity;
        });
      }).get();
    } catch (final InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
