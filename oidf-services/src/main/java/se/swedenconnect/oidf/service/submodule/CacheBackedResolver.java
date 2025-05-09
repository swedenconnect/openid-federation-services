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
package se.swedenconnect.oidf.service.submodule;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.resolver.DiscoveryRequest;
import se.swedenconnect.oidf.resolver.DiscoveryResponse;
import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.service.cache.managed.ManagedCache;

import java.util.Objects;

/**
 * Resolver Wrapper that uses a cache for requests.
 *
 * @author Felix Hellman
 */
public class CacheBackedResolver implements Resolver {

  private final Resolver inner;
  private final ManagedCache<String, String> cache;

  /**
   * Constructor.
   * @param inner for requests
   * @param cache for requests
   */
  public CacheBackedResolver(
      final Resolver inner,
      final ManagedCache<String, String> cache) {

    this.inner = inner;
    this.cache = cache;
  }

  @Override
  public String resolve(final ResolveRequest request) throws FederationException {
    final EntityID entityId = this.inner.getEntityId();
    final String cachedResponse = this.cache.get(request.toKey(entityId));
    if (Objects.nonNull(cachedResponse) && !cachedResponse.isBlank()) {
      return cachedResponse;
    }
    final String response = this.inner.resolve(request);
    this.cache.add(request.toKey(entityId), response);
    return response;
  }

  @Override
  public DiscoveryResponse discovery(final DiscoveryRequest request) {
    return this.inner.discovery(request);
  }

  @Override
  public EntityID getEntityId() {
    return this.inner.getEntityId();
  }
}
