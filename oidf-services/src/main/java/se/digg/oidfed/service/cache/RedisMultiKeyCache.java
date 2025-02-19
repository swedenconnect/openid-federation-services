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
import se.digg.oidfed.common.entity.integration.CacheRequest;
import se.digg.oidfed.common.entity.integration.MultiKeyCache;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisMultiKeyCache<V> implements MultiKeyCache<V> {

  private final RedisTemplate<String, V> template;
  private final RedisTemplate<String, String> stringTemplate;
  private final Class<V> vClass;

  public RedisMultiKeyCache(final RedisTemplate<String, V> template, final RedisTemplate<String, String> stringTemplate, final Class<V> vClass) {
    this.template = template;
    this.stringTemplate = stringTemplate;
    this.vClass = vClass;
  }

  @Override
  public void add(final String primaryKey, final Map<String, String> subKeys, final V value) {
    this.template.opsForValue().set(primaryKey, value);
    this.stringTemplate.opsForSet().add(this.getInternalPrimaryKey(), primaryKey);
    subKeys.forEach((k,v) -> {
      this.stringTemplate.opsForHash().put(k, v, primaryKey);
    });
  }

  private String getInternalPrimaryKey() {
    return "pk:%s".formatted(this.vClass.getCanonicalName());
  }

  @Override
  public void add(final String primaryKey, final V value) {
    this.add(primaryKey, Map.of(), value);
  }

  @Override
  public V get(final CacheRequest request) {
    if (request.isPrimaryKeySearch()) {
      return this.template.opsForValue().get(request.key());
    }
    final String primaryKey = (String) this.stringTemplate.opsForHash().get(request.keyName(), request.key());
    return Optional.ofNullable(primaryKey)
        .map(pk -> this.template.opsForValue().get(pk))
        .orElse(null);
  }

  @Override
  public Set<String> getPrimaryKeys() {
    return this.stringTemplate.opsForSet().members(this.getInternalPrimaryKey());
  }

  @Override
  public Set<String> getSubKeys(final String subKeyName) {
    return this.stringTemplate.opsForHash().keys(subKeyName).stream()
        .map(String.class::cast)
        .collect(Collectors.toSet());
  }
}
