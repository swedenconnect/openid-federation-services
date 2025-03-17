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

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.Function;

/**
 * Redis Request Response cache for resolver.
 *
 * @author Felix Hellman
 */
public class RedisModuleRequestResponseCache implements ResolverRequestResponseModuleCache {
  private final RedisTemplate<String, RequestResponseEntry> template;
  private final RedisTemplate<String, String> hitsTemplate;
  private final Function<String, String> resolve;

  /**
   * Constructor.
   * @param template for entries
   * @param hitsTemplate for request hits
   * @param resolve for re-running a query.
   */
  public RedisModuleRequestResponseCache(
      final RedisTemplate<String, RequestResponseEntry> template,
      final RedisTemplate<String, String> hitsTemplate,
      final Function<String, String> resolve) {

    this.template = template;
    this.hitsTemplate = hitsTemplate;
    this.resolve = resolve;
  }

  @Override
  public void add(final RequestResponseEntry requestResponseEntry) {
    this.template.opsForValue().set(requestResponseEntry.getRequest(), requestResponseEntry,
        Duration.between(Instant.now(), Instant.now().plus(60, ChronoUnit.SECONDS)));
    this.hitsTemplate.opsForZSet().incrementScore("resolver", requestResponseEntry.getRequest(), 1);
  }


  @Override
  public Set<String> flushRequestKeys() {
    final Set<String> requests = this.hitsTemplate.opsForZSet().rangeByScore("requests", 0.9, Double.MAX_VALUE);
    this.hitsTemplate.opsForZSet().remove("requests");
    return requests;
  }

  @Override
  public RequestResponseEntry get(final String key) {
    return this.template.opsForValue().get(key);
  }

  @Override
  public String resolve(final String request) {
    return this.resolve.apply(request);
  }
}
