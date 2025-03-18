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

import java.util.Objects;
import java.util.Optional;

/**
 * Manages service state distributed via redis.
 *
 * @author Felix Hellman
 */
public class RedisFederationServiceState implements FederationServiceState {

  private final RedisTemplate<String, String> template;

  /**
   * @param template for state
   */
  public RedisFederationServiceState(final RedisTemplate<String, String> template) {
    this.template = template;
  }

  @Override
  public void updateRegistryState(final String stateHash) {
    this.template.opsForValue().set("registry-sha256", stateHash);
  }

  private String getRegistrySha256() {
    return this.template.opsForValue().get("registry-sha256");
  }

  @Override
  public Boolean isStateMissing() {
    final String registrySha256 = this.getRegistrySha256();
    return Objects.isNull(registrySha256) || registrySha256.isBlank();
  }

  @Override
  public String getRegistryState() {
    return this.getRegistrySha256();
  }
}
