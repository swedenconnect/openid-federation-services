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
package se.swedenconnect.oidf.service.state;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis implementation of {@link ServiceLock}
 *
 * @author Felix Hellman
 */
public class RedisServiceLock implements ServiceLock {
  private final RedisTemplate<String, String> locks;

  /**
   * @param locks template
   */
  public RedisServiceLock(final RedisTemplate<String, String> locks) {
    this.locks = locks;
  }

  @Override
  public boolean acquireLock(final String name) {
    final Boolean lock = this.locks
        .opsForValue()
        .setIfAbsent(name, "lock", Duration.ofSeconds(20));
    return Optional.ofNullable(lock).orElse(false);
  }

  @Override
  public void close(final String name) {
    this.locks
        .delete(name);
  }
}
