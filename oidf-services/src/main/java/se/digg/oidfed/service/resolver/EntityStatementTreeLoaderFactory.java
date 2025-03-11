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
package se.digg.oidfed.service.resolver;

import org.springframework.context.ApplicationEventPublisher;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;
import se.digg.oidfed.resolver.tree.resolution.ErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;
import se.digg.oidfed.resolver.tree.resolution.ScheduledStepRecoveryStrategy;
import se.digg.oidfed.service.resolver.cache.TreeUpdatedEvent;

import java.util.concurrent.Executors;

/**
 * Entity statement tree factory class.
 *
 * @author Felix Hellman
 */
public class EntityStatementTreeLoaderFactory {
  private final FederationClient client;
  private final ExecutionStrategy executionStrategy;
  private final ErrorContextFactory errorContextFactory;
  private final ApplicationEventPublisher publisher;

  /**
   * @param client              for fetching entities
   * @param executionStrategy   for executing iterations
   * @param errorContextFactory for creating error context
   * @param publisher           publisher of events.
   */
  public EntityStatementTreeLoaderFactory(final FederationClient client,
                                          final ExecutionStrategy executionStrategy,
                                          final ErrorContextFactory errorContextFactory, final ApplicationEventPublisher publisher) {
    this.client = client;
    this.executionStrategy = executionStrategy;
    this.errorContextFactory = errorContextFactory;
    this.publisher = publisher;
  }

  /**
   * Creates a new EntityStatementTreeLoader
   *
   * @param properties for the loader
   * @return new instance of a tree loader
   */
  public EntityStatementTreeLoader create(final ResolverProperties properties) {
    return new EntityStatementTreeLoader(this.client, this.executionStrategy,
        new ScheduledStepRecoveryStrategy(Executors.newSingleThreadScheduledExecutor(), properties),
        this.errorContextFactory)
        .withAdditionalPostHook(() -> this.publisher.publishEvent(new TreeUpdatedEvent(properties.alias())));
  }

}
