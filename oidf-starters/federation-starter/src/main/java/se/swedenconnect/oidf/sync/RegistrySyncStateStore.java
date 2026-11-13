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

import java.util.Optional;
import java.util.UUID;

/**
 * Storage of the active {@link RegistrySyncState} of an instance.
 *
 * @author Felix Hellman
 */
public interface RegistrySyncStateStore {
  /**
   * @param instanceId of the instance
   * @return the active state, or empty if there is none
   */
  Optional<RegistrySyncState> load(final UUID instanceId);

  /**
   * @param instanceId of the instance
   * @return the sequence number of the active state, or empty if there is none
   */
  default Optional<Long> seq(final UUID instanceId) {
    return this.load(instanceId).map(RegistrySyncState::seq);
  }

  /**
   * Replaces the active state. Readers see either the previous or the new state, never a mix.
   *
   * @param instanceId of the instance
   * @param state      new active state
   */
  void store(final UUID instanceId, final RegistrySyncState state);
}
