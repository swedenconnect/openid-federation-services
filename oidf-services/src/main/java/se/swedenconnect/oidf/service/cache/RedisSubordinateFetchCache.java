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

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.SubordinateFetchCache;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link SubordinateFetchCache}.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisSubordinateFetchCache implements SubordinateFetchCache {

  private final RedisTemplate<String, String> template;

  @Override
  public Optional<String> get(final long snapshot, final FetchRequest request) {
    final String key = "subordinate-fetch:%d:%s".formatted(snapshot, request.subject());
    return Optional.ofNullable(this.template.opsForValue().get(key));
  }

  @Override
  public void put(final long snapshot, final FetchRequest request, final String response) {
    final String key = "subordinate-fetch:%d:%s".formatted(snapshot, request.subject());
    this.template.opsForValue().set(key, response);
    this.template.expire(key, Duration.ofHours(2));
  }
}
