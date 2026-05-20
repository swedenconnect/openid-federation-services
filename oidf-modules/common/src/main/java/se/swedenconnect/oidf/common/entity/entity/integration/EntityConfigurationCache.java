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

import java.util.Optional;

/**
 * Cache for entity configuration responses keyed by snapshot version and entity ID.
 *
 * @author Felix Hellman
 */
public interface EntityConfigurationCache {

  /**
   * Gets a cached entity configuration response.
   *
   * @param snapshot the snapshot version
   * @param entityId the entity identifier used as cache key
   * @return the cached serialized JWT, or empty if not found
   */
  Optional<String> get(long snapshot, String entityId);

  /**
   * Stores an entity configuration response in the cache.
   *
   * @param snapshot the snapshot version
   * @param entityId the entity identifier used as cache key
   * @param response the serialized JWT to cache
   */
  void put(long snapshot, String entityId, String response);
}
