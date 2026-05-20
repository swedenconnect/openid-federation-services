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
 * Cache for trust mark responses keyed by snapshot version, trust mark type and subject.
 *
 * @author Felix Hellman
 */
public interface TrustMarkCache {

  /**
   * Gets a cached trust mark response.
   *
   * @param snapshot       the snapshot version
   * @param trustMarkType  the trust mark type used as part of the cache key
   * @param subject        the subject used as part of the cache key
   * @return the cached response, or empty if not found
   */
  Optional<String> get(final long snapshot, final String trustMarkType, final String subject);

  /**
   * Stores a trust mark response in the cache.
   *
   * @param snapshot       the snapshot version
   * @param trustMarkType  the trust mark type used as part of the cache key
   * @param subject        the subject used as part of the cache key
   * @param response       the response to cache
   */
  void put(final long snapshot, final String trustMarkType, final String subject, final String response);
}
