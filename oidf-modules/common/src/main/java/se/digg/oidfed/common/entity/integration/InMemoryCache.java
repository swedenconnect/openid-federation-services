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
package se.digg.oidfed.common.entity.integration;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @param <K> key class
 * @param <V> value class
 *
 * @author Felix Hellman
 */
public class InMemoryCache<K extends Serializable, V> implements Cache<K, V> {

  private final ConcurrentHashMap<K, Expirable<V>> cache = new ConcurrentHashMap<>();

  private final Clock clock;

  /**
   * @param clock for time keeping
   */
  public InMemoryCache(final Clock clock) {
    this.clock = clock;
  }

  @Override
  public void add(final K key, final Expirable<V> value) {
    this.cache.put(key, value);
  }

  @Override
  public V get(final K key) {
    return Optional.ofNullable(this.cache.get(key))
        .map(Expirable::getValue)
        .orElse(null);
  }

  @Override
  public boolean shouldRefresh(final K key) {
    final Expirable<V> v = this.cache.get(key);
    return Objects.isNull(v) || v.getIssuedAt().isAfter(Instant.now(this.clock).plus(1, ChronoUnit.HOURS));
  }
}
