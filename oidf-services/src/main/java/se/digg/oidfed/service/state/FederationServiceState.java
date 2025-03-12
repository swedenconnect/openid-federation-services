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

/**
 * Handles service state between multiple components and instances.
 *
 * @author Felix Hellman
 */
public interface FederationServiceState {
  /**
   * @return true if no state has been initialized.
   */
  Boolean isStateMissing();

  /**
   * @param stateHash to set
   */
  void updateRegistryState(final String stateHash);

  /**
   * @return current state
   */
  String getRegistryState();

  /**
   * @param stateHash to check for
   * @return true if resolver is outdated
   */
  Boolean resolverNeedsReevaulation(final String stateHash);

  /**
   * @param stateHash to update with
   */
  void updateResolverState(final String stateHash);
}
