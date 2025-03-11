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
package se.digg.oidfed.service.state;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RedisFederationServiceState implements FederationServiceState {

  private final RedisTemplate<String, String> template;
  private final UUID randomizedIdentifier = UUID.randomUUID();

  public RedisFederationServiceState(final RedisTemplate<String, String> template) {
    this.template = template;
  }

  private void updateParticipation() {
    this.template.opsForZSet().add(
        "participation",
        this.randomizedIdentifier.toString(),
        Instant.now().getEpochSecond());
    // Remove old node participation's
    this.template.opsForZSet().removeRangeByScore(
        "participation", -1, Instant.now().getEpochSecond() - 60
    );
  }

  @Override
  public void updateRegistryState(final String stateHash) {
    this.updateParticipation();
    //Registry state is shared.
    this.template.opsForValue().set("registry-sha256", stateHash);
  }

  private String getRegistrySha256() {
    this.updateParticipation();
    return this.template.opsForValue().get("registry-sha256");
  }

  private String getRouterSha256(final String participant) {
    this.updateParticipation();
    return this.template.opsForValue().get("%s:router-sha256".formatted(participant));
  }

  @Override
  public Boolean isRouterStatesEqual() {
    this.updateParticipation();
    final Set<String> routerSha = Optional.ofNullable(this.template.opsForZSet()
            .range("participation", Long.MIN_VALUE, Long.MAX_VALUE))
        .orElse(Set.of())
        .stream().map(this::getRouterSha256)
        .collect(Collectors.toSet());

    return routerSha.size() == 1;
  }

  @Override
  public void updateRouterState(final String stateHash) {
    this.updateParticipation();
    this.template.opsForValue().set(
        "%s:router-sha256".formatted(this.randomizedIdentifier.toString()),
        stateHash
    );
  }

  @Override
  public Boolean resolverNeedsReevaulation() {
    this.updateParticipation();
    if (!this.isRouterStatesEqual()) {
      //Resolver probably needs to be reloaded, but we need to wait
      return false;
    }

    final String resolverSha = this.template.opsForValue().get("resolver-sha256");
    final String routerSha = this.getRouterSha256(this.randomizedIdentifier.toString());

    return !Optional.ofNullable(resolverSha).orElse("").equals(routerSha);
  }

  @Override
  public void updateResolverState(final String stateHash) {
    this.updateParticipation();
    this.template.opsForValue().set("resolver-sha256", stateHash);
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

  @Override
  public Boolean isRouterStateCurrent(final String stateHash) {
    if (Objects.isNull(stateHash)) {
      return false;
    }
    return stateHash.equals(this.getRouterState());
  }

  @Override
  public String getRouterState() {
    return this.getRouterSha256(this.randomizedIdentifier.toString());
  }
}
