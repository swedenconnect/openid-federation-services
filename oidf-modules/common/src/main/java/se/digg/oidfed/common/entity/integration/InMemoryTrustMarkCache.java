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

import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementation of trust mark cache.
 *
 * @author Felix Hellman
 */
@Slf4j
public class InMemoryTrustMarkCache {

  private final Map<TrustMarkRequest, CacheResponse<SignedJWT>> cache = new ConcurrentHashMap<>();

  /**
   * @param request to get
   * @return cache response
   */
  public CacheResponse<SignedJWT> get(final TrustMarkRequest request) {
    final CacheResponse<SignedJWT> cacheResponse = this.cache.get(request);
    if (Objects.isNull(cacheResponse)) {
      return new CacheResponse<>(Optional.empty(), true);
    }

    if (this.hasExpired(cacheResponse)) {
      this.cache.remove(request);
      return new CacheResponse<>(Optional.empty(), true);
    }
    return cacheResponse;
  }

  /**
   * @param request key
   * @param jwt     value
   */
  public void add(final TrustMarkRequest request, @Nullable final SignedJWT jwt) {
    this.cache.put(request, new CacheResponse<>(Optional.ofNullable(jwt), Objects.nonNull(jwt)));
  }

  private boolean hasExpired(final CacheResponse<SignedJWT> response) {
    final Optional<Boolean> expired = response
        .data()
        .map(data -> {
          try {
            return data.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(Instant.now());
          } catch (final Exception e) {
            log.warn("Failed to parse cached value to determine expiration, returning true", e);
            //Returning true will remove this poisned value
            return true;
          }
        });
    return expired.isPresent() && expired.get();
  }
}
