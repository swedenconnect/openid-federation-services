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
package se.digg.oidfed.service.resolver.cache;

import se.digg.oidfed.common.entity.integration.properties.ResolverProperties;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;

/**
 * Registartion of a cache.
 *
 * @param tree       of the cache
 * @param loader     for the cache
 * @param cache      the actual cache
 * @param properties properties for the cache
 * @author Felix Hellman
 */
public record ResolverCacheRegistration(EntityStatementTree tree, EntityStatementTreeLoader loader,
                                        ResolverCache cache, ResolverProperties properties) {
}
