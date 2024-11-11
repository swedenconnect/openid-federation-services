/*
 * Copyright 2024 Sweden Connect
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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.digg.oidfed.resolver.ResolverProperties;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.resolver.tree.CacheSnapshot;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;
import se.digg.oidfed.resolver.tree.SearchRequest;
import se.digg.oidfed.resolver.tree.Tree;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled tasks for the resolver.
 *
 * @author Felix Hellman
 */
@Component
public class ResolverScheduledTasks {

  private final Tree<EntityStatement> tree;
  private final EntityStatementTreeLoader loader;
  private final ResolverProperties properties;
  private final EntityStatementIntegration integration;

  /**
   * Constructor
   * @param tree to perform actions on
   * @param loader to reload tree with
   * @param properties for finding trustAnchor
   * @param integration to fetch entity statements
   */
  public ResolverScheduledTasks(final Tree<EntityStatement> tree, final EntityStatementTreeLoader loader,
      final ResolverProperties properties, final EntityStatementIntegration integration) {
    this.tree = tree;
    this.loader = loader;
    this.properties = properties;
    this.integration = integration;
  }

  /**
   * Reloads individual entities in the tree at an interval
   */
  @Scheduled(fixedRate = 10L, timeUnit = TimeUnit.MINUTES)
  public void refreshEntities() {
    final CacheSnapshot<EntityStatement> snapshot = tree.getCurrentSnapshot();
    tree.search(new SearchRequest<>((a, s) -> true, false, snapshot))
        .forEach(result -> {
          final EntityStatement entityStatement = integration.getEntityStatement(result.node().getKey());
          result.context().cacheSnapshot().setData(result.node().getKey(), entityStatement);
        });
  }

  /**
   * Reloads the whole tree at an interval
   */
  @Scheduled(fixedRate = 60L, timeUnit = TimeUnit.MINUTES)
  public void refreshTree() {
    loader.resolveTree(properties.trustAnchor(), tree);
  }
}
