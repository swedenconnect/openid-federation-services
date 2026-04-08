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

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.CachedResponse;
import se.swedenconnect.oidf.common.entity.entity.integration.ModuleResponseCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link ModuleResponseCache}.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisModuleResponseCache implements ModuleResponseCache {

  private final RedisTemplate<String, String> template;
  private final Duration cacheTtl;
  private final Gson gson;

  @Override
  public Optional<CachedResponse> get(final long snapshot, final String requestUri) {
    final String key = "module-response:%d:%s"
        .formatted(snapshot, URLEncoder.encode(requestUri, StandardCharsets.UTF_8));
    return Optional.ofNullable(this.template.opsForValue().get(key))
        .map(json -> this.gson.fromJson(json, CachedResponse.class));
  }

  @Override
  public void put(final long snapshot, final String requestUri, final CachedResponse response) {
    final String key = "module-response:%d:%s"
        .formatted(snapshot, URLEncoder.encode(requestUri, StandardCharsets.UTF_8));
    this.template.opsForValue().set(key, this.gson.toJson(response), this.cacheTtl);
  }
}
