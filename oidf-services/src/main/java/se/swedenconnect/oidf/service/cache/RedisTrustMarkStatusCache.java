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
package se.swedenconnect.oidf.service.cache;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.AllArgsConstructor;
import org.hamcrest.core.Is;
import org.springframework.data.redis.core.RedisTemplate;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.TrustMarkStatusCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.trustmarkissuer.TrustMarkSigner;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link TrustMarkStatusCache}.
 *
 * @author Felix Hellman
 */
@AllArgsConstructor
public class RedisTrustMarkStatusCache implements TrustMarkStatusCache {

  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public Optional<String> get(final long snapshot, final String trustMarkJwt) {
    return Optional.ofNullable(this.redisTemplate.opsForValue().get("trust-mark-status:%d:%s".formatted(snapshot,
        trustMarkJwt)));
  }

  @Override
  public void put(final long snapshot, final String trustMarkJwt, final String response) {
    final String key = "trust-mark-status:%d:%s".formatted(snapshot, trustMarkJwt);
    this.redisTemplate.opsForValue().set(key, response);
    this.redisTemplate.expire(key, Duration.ofHours(2));
  }
}
