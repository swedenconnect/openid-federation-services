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
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link TrustMarkCache}.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisTrustMarkCache implements TrustMarkCache {

  private final RedisTemplate<String, String> template;
  private final Duration cacheTtl;

  @Override
  public Optional<String> get(final long snapshot, final String trustMarkType, final String subject) {
    final String key = "trust-mark:%d:%s:%s".formatted(snapshot, URLEncoder.encode(trustMarkType, StandardCharsets.UTF_8), URLEncoder.encode(subject, StandardCharsets.UTF_8));
    return Optional.ofNullable(this.template.opsForValue().get(key));
  }

  @Override
  public void put(final long snapshot, final String trustMarkType, final String subject, final String response) {
    final String key = "trust-mark:%d:%s:%s".formatted(snapshot, URLEncoder.encode(trustMarkType, StandardCharsets.UTF_8), URLEncoder.encode(subject, StandardCharsets.UTF_8));
    this.template.opsForValue().set(key, response);
    this.template.expire(key, this.cacheTtl);
  }
}
