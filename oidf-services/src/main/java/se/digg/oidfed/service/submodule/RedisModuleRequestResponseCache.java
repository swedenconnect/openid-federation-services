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
package se.digg.oidfed.service.submodule;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/**
 * Redis Request Response cache for resolver.
 *
 * @author Felix Hellman
 */
public class RedisModuleRequestResponseCache implements RequestResponseModuleCache {
  private final RedisTemplate<String, RequestResponseEntry> template;
  private final RedisTemplate<String, String> hitsTemplate;
  private final EntityID entityID;
  private final Integer threshold;

  /**
   * Constructor.
   *
   * @param template     for entries
   * @param hitsTemplate for request hits
   * @param entityID     for cache
   * @param threshold    for cache evaluation
   */
  public RedisModuleRequestResponseCache(
      final RedisTemplate<String, RequestResponseEntry> template,
      final RedisTemplate<String, String> hitsTemplate,
      final EntityID entityID,
      final Integer threshold) {

    this.template = template;
    this.hitsTemplate = hitsTemplate;
    this.entityID = entityID;
    this.threshold = threshold;
  }

  @Override
  public void add(final RequestResponseEntry requestResponseEntry) {
    this.template.opsForValue().set(requestResponseEntry.getRequest(), requestResponseEntry,
        Duration.between(Instant.now(), Instant.now().plus(60, ChronoUnit.SECONDS)));
    this.hitsTemplate.opsForZSet().incrementScore(this.getHitsKey(), requestResponseEntry.getRequest(), 1);
  }


  @Override
  public Set<String> flushRequestKeys() {
    final Set<String> requests = this.hitsTemplate.opsForZSet().rangeByScore("requests", this.threshold - 0.1,
        Double.MAX_VALUE);
    this.hitsTemplate.opsForZSet().remove(this.getHitsKey());
    return requests;
  }

  @Override
  public RequestResponseEntry get(final String key) {
    final RequestResponseEntry value = this.template.opsForValue().get(key);
    if (Objects.nonNull(value)) {
      this.hitsTemplate.opsForZSet().incrementScore(this.getHitsKey(), key, 1);
    }
    return value;
  }

  private String getHitsKey() {
    return "%s:requests".formatted(this.entityID.getValue());
  }
}
