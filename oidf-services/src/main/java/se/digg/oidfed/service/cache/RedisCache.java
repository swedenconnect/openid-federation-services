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
package se.digg.oidfed.service.cache;

import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Redis implementation for {@link  Cache}
 * @param <K> key class
 * @param <V> value class
 *
 * @author Felix Hellman
 */
public class RedisCache<K extends Serializable, V> implements Cache<K, V> {

  protected final RedisTemplate<K, Expirable<V>> valueTemplate;
  protected final Clock clock;

  /**
   * Constructor
   * @param valueTemplate for redis
   * @param clock for keeping time
   */
  public RedisCache(final RedisTemplate<K, Expirable<V>> valueTemplate, final Clock clock) {
    this.valueTemplate = valueTemplate;
    this.clock = clock;
  }

  @Override
  public void add(final K key, final Expirable<V> value) {
    this.valueTemplate.opsForValue().set(key, value);
  }

  @Override
  public V get(final K key) {
    return Optional.ofNullable(this.valueTemplate.opsForValue().get(key))
        .map(Expirable::getValue)
        .orElse(null);
  }

  @Override
  public boolean shouldRefresh(final K key) {
    return Optional.ofNullable(this.valueTemplate.opsForValue().get(key)).map(Expirable::getExpiration)
        .map(exp -> exp.isAfter(Instant.now(this.clock)))
        .orElse(true);
  }
}
