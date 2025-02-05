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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.ListCache;

import java.io.Serializable;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis Impementation for {@link CacheFactory}
 *
 * @author Felix Hellman
 */
public class RedisCacheFactory implements CacheFactory {

  private final Clock clock;
  private final RedisConnectionFactory factory;

  /**
   * Constructor.
   * @param clock for keeping time
   * @param factory for conenctions
   */
  public RedisCacheFactory(final Clock clock, final RedisConnectionFactory factory) {
    this.clock = clock;
    this.factory = factory;
  }

  private final Map<Class<?>, RedisSerializer<?>> serializerMap = Map.of(
      EntityStatement.class, new ExpirableEntityStatementSerializer()
  );

  @Override
  public <K extends Serializable, V> Cache<K, V> create(final Class<V> v) {
    final RedisTemplate<K, Expirable<V>> template = new RedisTemplate<>();
    template.setConnectionFactory(this.factory);
    Optional.ofNullable(this.serializerMap.get(v)).ifPresent(template::setValueSerializer);
    template.afterPropertiesSet();
    return new RedisCache<>(template, this.clock);
  }

  @Override
  public <K extends Serializable, V> Cache<K, List<V>> createListValueCache(final Class<V> v) {
    final RedisTemplate<K, Expirable<List<V>>> template = new RedisTemplate<>();
    template.setConnectionFactory(this.factory);
    Optional.ofNullable(this.serializerMap.get(v)).ifPresent(template::setValueSerializer);
    template.afterPropertiesSet();
    return new RedisCache<>(template, this.clock);
  }

  @Override
  public <K extends Serializable, V> ListCache<K, V> createListCache(final Class<V> v) {
    final RedisTemplate<K, V> template = new RedisTemplate<>();
    template.setConnectionFactory(this.factory);
    Optional.ofNullable(this.serializerMap.get(v)).ifPresent(template::setValueSerializer);
    template.afterPropertiesSet();
    return new RedisListCache<>(template);
  }
}
