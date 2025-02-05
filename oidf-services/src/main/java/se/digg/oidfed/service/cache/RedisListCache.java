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
import se.digg.oidfed.common.entity.integration.ListCache;

import java.io.Serializable;
import java.util.List;

/**
 * Redis implementation for {@link ListCache}
 * @param <K> key class
 * @param <V> value class
 *
 * @author Felix Hellman
 */
public class RedisListCache<K extends Serializable, V> implements ListCache<K,V> {

  private final RedisTemplate<K,V> template;

  /**
   * Constructor.
   * @param template for redis
   */
  public RedisListCache(final RedisTemplate<K, V> template) {
    this.template = template;
  }

  @Override
  public void append(final K key, final V value) {
    this.template.opsForList().rightPush(key, value);
  }

  @Override
  public List<V> getAll(final K key) {
    return this.template.opsForList().range(key,0L, Long.MAX_VALUE);
  }
}
