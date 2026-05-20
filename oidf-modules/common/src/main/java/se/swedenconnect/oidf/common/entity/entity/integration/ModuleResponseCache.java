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
 * Cache for module responses keyed by snapshot version and request URI.
 *
 * @author Felix Hellman
 */
public interface ModuleResponseCache {

  /**
   * Gets a cached module response.
   *
   * @param snapshot   the snapshot version
   * @param requestUri the full request URI (including query parameters) used as cache key
   * @return the cached response, or empty if not found
   */
  Optional<CachedResponse> get(long snapshot, String requestUri);

  /**
   * Stores a module response in the cache.
   *
   * @param snapshot   the snapshot version
   * @param requestUri the full request URI (including query parameters) used as cache key
   * @param response   the response to cache
   */
  void put(long snapshot, String requestUri, CachedResponse response);
}
