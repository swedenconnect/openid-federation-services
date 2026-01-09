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
package se.swedenconnect.oidf.resolver;

import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.tree.ResolverCache;
import se.swedenconnect.oidf.common.entity.tree.VersionedInMemoryCache;

/**
 * Resolver cache factory for in memory caches.
 *
 * @author Felix Hellman
 */
public class InMemoryResolverCacheFactory implements ResolverCacheFactory {

  @Override
  public ResolverCache create(final ResolverProperties properties) {
    return new VersionedInMemoryCache();
  }
}
