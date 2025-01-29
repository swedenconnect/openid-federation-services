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
package se.digg.oidfed.common.entity.integration;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Loading cache for getting {@link TrustMarkEntry} from Trust Mark Issuers.
 *
 * @author Felix Hellman
 */
@Slf4j
public class TrustMarkLoadingCache {

  /**
   * Constructor
   * @param cache underlying
   * @param integration to fetch new values
   */
  public TrustMarkLoadingCache(final InMemoryTrustMarkCache cache, final TrustMarkIntegration integration) {
    this.cache = cache;
    this.integration = integration;
  }

  private final InMemoryTrustMarkCache cache;
  private final TrustMarkIntegration integration;

  /**
   * @param request for a trust mark
   * @return trust mark
   */
  public Optional<TrustMarkEntry> getTrustMark(final TrustMarkRequest request) {
    final CacheResponse<SignedJWT> cacheResponse = this.cache.get(request);
    if (cacheResponse.data().isPresent()) {
      final TrustMarkEntry trustMarkEntry = new TrustMarkEntry(request.trustMarkId(), cacheResponse.data().get());
      return Optional.of(trustMarkEntry);
    }
    if (cacheResponse.fetchOnMiss()) {
      final Optional<SignedJWT> fromIntegration = this.getFromIntegration(request);
      fromIntegration.ifPresent(jwt -> this.cache.add(request, jwt));
      return fromIntegration.map(jwt -> new TrustMarkEntry(request.trustMarkId(), jwt));
    }
    return Optional.empty();
  }

  private Optional<SignedJWT> getFromIntegration(final TrustMarkRequest request) {
    try {
      return Optional.of(this.integration.getTrustMark(request));
    } catch (final Exception e) {
      log.error("Failed to fetch expected trust mark", e);
      return Optional.empty();
    }
  }
}
