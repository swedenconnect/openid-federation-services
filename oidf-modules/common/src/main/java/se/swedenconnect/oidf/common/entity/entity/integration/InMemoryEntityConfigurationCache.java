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
package se.swedenconnect.oidf.common.entity.entity.integration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link EntityConfigurationCache}.
 *
 * @author Felix Hellman
 */
public class InMemoryEntityConfigurationCache implements EntityConfigurationCache {

  private final Map<Long, Map<String, String>> cache = new ConcurrentHashMap<>();

  @Override
  public Optional<String> get(final long snapshot, final String entityId) {
    return Optional.ofNullable(this.cache.get(snapshot))
        .flatMap(version -> Optional.ofNullable(version.get(entityId)));
  }

  @Override
  public void put(final long snapshot, final String entityId, final String response) {
    this.cache.computeIfAbsent(snapshot, _ -> new ConcurrentHashMap<>()).put(entityId, response);
  }
}
