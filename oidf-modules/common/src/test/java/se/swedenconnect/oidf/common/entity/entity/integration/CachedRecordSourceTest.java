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
package se.swedenconnect.oidf.common.entity.entity.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests that {@link CachedRecordSource} refreshes its local record from one thread at a time.
 *
 * @author Felix Hellman
 */
class CachedRecordSourceTest {

  private static final int THREADS = 16;

  @Test
  void staleRecordIsRefreshedByOneThreadWhileOthersUseTheCurrentRecord() throws Exception {
    final CompositeRecord current = record();
    final BlockingCache cache = new BlockingCache(record());
    final CachedRecordSource source = new CachedRecordSource(cache, Duration.ZERO);
    source.addRecord(current);

    try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final List<Future<List<TrustAnchorProperties>>> results = new ArrayList<>();
      for (int i = 0; i < THREADS; i++) {
        results.add(executor.submit(source::getTrustAnchorProperties));
      }
      Assertions.assertTrue(cache.entered.await(5, TimeUnit.SECONDS));

      // Every thread except the one refreshing completes with the current record without waiting
      int completedWithCurrent = 0;
      for (final Future<List<TrustAnchorProperties>> result : results) {
        try {
          Assertions.assertSame(trustAnchors(current), result.get(1, TimeUnit.SECONDS));
          completedWithCurrent++;
        }
        catch (final java.util.concurrent.TimeoutException e) {
          // the refreshing thread, blocked in the cache
        }
      }
      Assertions.assertEquals(THREADS - 1, completedWithCurrent);
      Assertions.assertEquals(1, cache.gets.get());
      cache.release.countDown();
    }
  }

  @Test
  void threadsWithoutARecordWaitForASingleLoad() throws Exception {
    final CompositeRecord loaded = record();
    final BlockingCache cache = new BlockingCache(loaded);
    final CachedRecordSource source = new CachedRecordSource(cache, Duration.ofMinutes(1));

    try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final List<Future<List<TrustAnchorProperties>>> results = new ArrayList<>();
      for (int i = 0; i < THREADS; i++) {
        results.add(executor.submit(source::getTrustAnchorProperties));
      }
      Assertions.assertTrue(cache.entered.await(5, TimeUnit.SECONDS));
      cache.release.countDown();
      for (final Future<List<TrustAnchorProperties>> result : results) {
        Assertions.assertSame(trustAnchors(loaded), result.get(5, TimeUnit.SECONDS));
      }
    }
    Assertions.assertEquals(1, cache.gets.get());
  }

  private static CompositeRecord record() {
    final Instant now = Instant.now();
    final Instant expiration = now.plus(Duration.ofHours(1));
    final ModuleRecord module = ModuleRecord.builder()
        .trustAnchors(new ArrayList<>(List.of(new TrustAnchorProperties())))
        .build();
    return new CompositeRecord(new Expirable<>(expiration, now, List.of()), new Expirable<>(expiration, now, module));
  }

  private static List<TrustAnchorProperties> trustAnchors(final CompositeRecord record) {
    return record.getModuleRecord().getValue().getTrustAnchors();
  }

  /**
   * Cache that blocks in {@link #get(String)} until released, and counts the calls.
   */
  private static class BlockingCache implements Cache<String, CompositeRecord> {
    private final CompositeRecord value;
    private final AtomicInteger gets = new AtomicInteger();
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    BlockingCache(final CompositeRecord value) {
      this.value = value;
    }

    @Override
    public void add(final String key, final Expirable<CompositeRecord> value) {
    }

    @Override
    public CompositeRecord get(final String key) {
      this.gets.incrementAndGet();
      this.entered.countDown();
      try {
        this.release.await();
      }
      catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return this.value;
    }

    @Override
    public boolean shouldRefresh(final String key) {
      return false;
    }
  }
}
