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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkStatusCache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link TrustMarkStatusCache}.
 *
 * @author Felix Hellman
 */
@Slf4j
@AllArgsConstructor
public class RedisTrustMarkStatusCache implements TrustMarkStatusCache {

  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public Optional<String> get(final long snapshot, final String trustMarkJwt) {
    return Optional.ofNullable(this.redisTemplate.opsForValue().get("trust-mark-status:%d:%s".formatted(snapshot,
        sha256(trustMarkJwt))));
  }

  @Override
  public void put(final long snapshot, final String trustMarkJwt, final String response) {
    final String key = "trust-mark-status:%d:%s".formatted(snapshot, sha256(trustMarkJwt));
    this.redisTemplate.opsForValue().set(key, response);
    this.redisTemplate.expire(key, Duration.ofHours(2));
  }

  private static String sha256(final String jwt) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(jwt.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
