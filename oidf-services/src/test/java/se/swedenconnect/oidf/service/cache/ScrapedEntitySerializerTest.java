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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.scraping.FetchError;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedIntermediate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ScrapedEntitySerializer}.
 *
 * @author Martin Lindström
 */
class ScrapedEntitySerializerTest {

  private final ScrapedEntitySerializer serializer = new ScrapedEntitySerializer();

  @Test
  void scrapeDataIsKept() {
    final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    final TrustMarkStatusResponse status = new TrustMarkStatusResponse(null, false);
    status.setLastChecked(now);
    final Map<String, TrustMarkStatusResponse> statuses = new HashMap<>();
    statuses.put("tm", status);
    final ScrapedEntity entity = ScrapedEntity.builder()
        .entityID(new EntityID("https://example.com"))
        .lastSeen(now)
        .lastError(FetchError.httpStatus(503))
        .trustMarkStatuses(statuses)
        .intermediate(new ScrapedIntermediate(List.of("https://sub.example.com"), Map.of()))
        .build();

    final ScrapedEntity result = this.serializer.deserialize(this.serializer.serialize(entity));

    Assertions.assertEquals(now, result.getLastSeen());
    Assertions.assertEquals(FetchError.httpStatus(503), result.getLastError());
    Assertions.assertEquals(now, result.getTrustMarkStatuses().get("tm").getLastChecked());
    Assertions.assertEquals(List.of("https://sub.example.com"), result.getIntermediate().listing());
  }

  @Test
  void entryStoredBeforeScrapeDataHasNoLastSeen() {
    final String stored = """
        {"entityID":"https://example.com","scrapedAt":"2026-01-01T00:00:00Z",
         "scrapeFailedAt":"2026-01-01T00:00:00Z","trustMarkStatuses":{},"intermediate":{"subordinates":{}}}""";

    final ScrapedEntity result = this.serializer.deserialize(stored.getBytes(StandardCharsets.UTF_8));

    Assertions.assertNull(result.getLastSeen());
    Assertions.assertNull(result.getLastError());
    Assertions.assertNull(result.getIntermediate().listing());
  }
}
