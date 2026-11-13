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
package se.swedenconnect.oidf.sync;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory implementation of {@link RegistrySyncStateStore}, the state is held per node.
 *
 * @author Felix Hellman
 */
public class InMemoryRegistrySyncStateStore implements RegistrySyncStateStore {

  private final Map<UUID, RegistrySyncState> states = new ConcurrentHashMap<>();

  @Override
  public Optional<RegistrySyncState> load(final UUID instanceId) {
    return Optional.ofNullable(this.states.get(instanceId));
  }

  @Override
  public void store(final UUID instanceId, final RegistrySyncState state) {
    this.states.put(instanceId, state);
  }
}
