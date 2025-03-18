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
package se.digg.oidfed.service.cache.managed;

import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.resolver.ResolverFactory;

import java.util.List;

/**
 * Loader for modules.
 *
 * @author Felix Hellman
 */
public class ModuleLoader {
  private final ResolverFactory factory;
  private final CompositeRecordSource source;

  /**
   * @param factory for creating resolvers
   * @param source for finding properties
   */
  public ModuleLoader(final ResolverFactory factory, final CompositeRecordSource source) {
    this.factory = factory;
    this.source = source;
  }

  /**
   * @return list of all configured resolvers
   */
  public List<Resolver> getResolvers() {
    return this.source.getResolverProperties().stream()
        .map(this.factory::create)
        .toList();
  }
}
