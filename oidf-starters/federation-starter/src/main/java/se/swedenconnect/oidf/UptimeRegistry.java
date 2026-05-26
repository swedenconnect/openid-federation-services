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
package se.swedenconnect.oidf;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sliding-window uptime registry for per-entity reachability tracking.
 *
 * @author Felix Hellman
 */
public class UptimeRegistry {

  private static final int WINDOW_SIZE = 20;

  private final ConcurrentMap<String, Deque<Boolean>> reachabilityWindows = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Instant> lastSeenReachable = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> avgResponseTimes = new ConcurrentHashMap<>();

  /**
   * Records a fetch outcome for the given entity.
   *
   * @param entityId the entity identifier
   * @param success  true if the fetch succeeded
   */
  public void record(final String entityId, final boolean success) {
    final Deque<Boolean> window = this.reachabilityWindows.computeIfAbsent(
        entityId, k -> new ArrayDeque<>(WINDOW_SIZE + 1));
    synchronized (window) {
      window.addLast(success);
      if (window.size() > WINDOW_SIZE) {
        window.removeFirst();
      }
    }
  }

  /**
   * Records a successful fetch with response time and updates last-seen timestamp.
   *
   * @param entityId     the entity identifier
   * @param responseTimeMs response time in milliseconds
   */
  public void recordSuccess(final String entityId, final long responseTimeMs) {
    this.lastSeenReachable.put(entityId, Instant.now());
    this.avgResponseTimes.merge(entityId, responseTimeMs,
        (prev, next) -> (prev * 9 + next) / 10);
  }

  /**
   * Returns the uptime ratio (0.0–1.0) over the last N cycles for the given entity.
   * Returns empty if no data has been recorded yet.
   *
   * @param entityId the entity identifier
   * @return uptime ratio, or empty if no data
   */
  public Optional<Double> uptimeRatio(final String entityId) {
    final Deque<Boolean> window = this.reachabilityWindows.get(entityId);
    if (window == null || window.isEmpty()) {
      return Optional.empty();
    }
    synchronized (window) {
      final long successes = window.stream().filter(Boolean::booleanValue).count();
      return Optional.of((double) successes / window.size());
    }
  }

  /**
   * Returns the last time the entity was successfully reached, if ever.
   *
   * @param entityId the entity identifier
   * @return last seen instant, or empty
   */
  public Optional<Instant> getLastSeenReachable(final String entityId) {
    return Optional.ofNullable(this.lastSeenReachable.get(entityId));
  }

  /**
   * Returns the exponential moving average response time in milliseconds, or empty if no data.
   *
   * @param entityId the entity identifier
   * @return average response time in ms, or empty
   */
  public Optional<Long> getAvgResponseMs(final String entityId) {
    return Optional.ofNullable(this.avgResponseTimes.get(entityId));
  }
}
