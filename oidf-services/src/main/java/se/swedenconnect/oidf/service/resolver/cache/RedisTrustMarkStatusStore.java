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
package se.swedenconnect.oidf.service.resolver.cache;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusStore;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-backed implementation of {@link TrustMarkStatusStore}.
 *
 * @author Felix Hellman
 */
public class RedisTrustMarkStatusStore implements TrustMarkStatusStore {

  private final RedisTemplate<String, TrustMarkStatusResponse> template;
  private final String namespace;

  /**
   * Constructor.
   *
   * @param template  for Redis operations
   * @param namespace resolver entity identifier used to scope keys
   */
  public RedisTrustMarkStatusStore(final RedisTemplate<String, TrustMarkStatusResponse> template, final String namespace) {
    this.template = template;
    this.namespace = namespace;
  }

  private String redisKey(final String subject, final String trustMarkType) {
    return "%s:tmstatus:%s::%s".formatted(this.namespace, subject, trustMarkType);
  }

  @Override
  public void setTrustMarkStatus(final String subject, final String trustMarkType, final TrustMarkStatusResponse response) {
    final String key = this.redisKey(subject, trustMarkType);
    this.template.opsForValue().set(key, response);
    try {
      final Duration defaultExpiration = Duration.ofHours(1);
      if (response.isError()) {
          this.template.expire(key, defaultExpiration);
      } else {
        final Duration expiration = Optional.ofNullable(Duration.between(Instant.now(),
                response.getSignedJWT().getJWTClaimsSet().getExpirationTime().toInstant()))
            .orElse(defaultExpiration);
        this.template.expire(key, expiration);
      }
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<TrustMarkStatusResponse> getTrustMarkStatus(final String subject, final String trustMarkType) {
    return Optional.ofNullable(this.template.opsForValue().get(this.redisKey(subject, trustMarkType)));
  }

  @Override
  public void clear() {
    final Set<String> keys = this.template.keys("%s:tmstatus:*".formatted(this.namespace));
    if (keys != null && !keys.isEmpty()) {
      this.template.delete(keys);
    }
  }
}
