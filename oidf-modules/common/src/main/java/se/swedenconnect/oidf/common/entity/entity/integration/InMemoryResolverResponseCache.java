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
package se.swedenconnect.oidf.common.entity.entity.integration;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ResolverResponseCache}.
 *
 * @author Felix Hellman
 */
public class InMemoryResolverResponseCache implements ResolverResponseCache {
  final Map<Long, Map<String, String>> resolverResponseCache = new ConcurrentHashMap<>();
  @Override
  public Optional<String> get(final long snapshot, final ResolveRequest request) {
    return Optional.ofNullable(this.resolverResponseCache.get(snapshot))
        .flatMap(cache -> Optional.ofNullable(cache.get(request.toKey(new EntityID(request.subject())))));
  }

  @Override
  public void put(final long snapshot, final ResolveRequest request, final String response) {
    this.resolverResponseCache.computeIfAbsent(snapshot, _ -> new ConcurrentHashMap<>())
        .put(request.toKey(new EntityID(request.subject())), response);
  }
}
