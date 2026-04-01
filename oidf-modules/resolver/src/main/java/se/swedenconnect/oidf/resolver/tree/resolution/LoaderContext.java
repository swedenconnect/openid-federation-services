package se.swedenconnect.oidf.resolver.tree.resolution;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class LoaderContext {
  private final Map<EntityID, CompletableFuture<ScrapedEntity>> scrapedEntities = new ConcurrentHashMap<>();

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
