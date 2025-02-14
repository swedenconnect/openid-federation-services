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

import com.nimbusds.jose.jwk.JWKSet;
import se.digg.oidfed.common.entity.integration.registry.ResolverProperties;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.common.tree.ResolverCache;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.resolver.ResolverResponseFactory;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.chain.ConstraintsValidationStep;
import se.digg.oidfed.resolver.chain.CriticalClaimsValidationStep;
import se.digg.oidfed.resolver.chain.SignatureValidationStep;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.service.resolver.cache.ResolverCacheFactory;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistration;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;

import java.time.Clock;
import java.util.List;

/**
 * Factory class for creating {@link Resolver} from {@link ResolverProperties}
 *
 * @author Felix Hellman
 */
public class ResolverFactory {

  private final ResolverCacheFactory resolverCacheFactory;
  private final MetadataProcessor processor;
  private final EntityStatementTreeLoaderFactory treeLoaderFactory;
  private final ResolverCacheRegistry registry;
  private final SignerFactory signerAdapter;

  /**
   * Constructor.
   *
   * @param resolverCacheFactory factory for creating snap resolver caches
   * @param processor            to use for metadata
   * @param treeLoaderFactory    to use for creating tree loaders
   * @param registry             for caches
   * @param signerAdapter        to use
   */
  public ResolverFactory(
      final ResolverCacheFactory resolverCacheFactory,
      final MetadataProcessor processor,
      final EntityStatementTreeLoaderFactory treeLoaderFactory,
      final ResolverCacheRegistry registry,
      final SignerFactory signerAdapter) {
    this.resolverCacheFactory = resolverCacheFactory;
    this.processor = processor;
    this.treeLoaderFactory = treeLoaderFactory;
    this.registry = registry;
    this.signerAdapter = signerAdapter;
  }

  /**
   * Creates a new instance of a {@link Resolver}
   *
   * @param properties to create a module from
   * @return new instance
   */
  public Resolver create(final ResolverProperties properties) {
    final ResolverCache entityStatementSnapshotSource = this.resolverCacheFactory.create(properties);
    final EntityStatementTree entityStatementTree = new EntityStatementTree(new Tree<>(entityStatementSnapshotSource));
    this.registry.registerCache(properties.alias(), new ResolverCacheRegistration(
        entityStatementTree,
        this.treeLoaderFactory.create(properties),
        entityStatementSnapshotSource,
        properties
    ));
    return new Resolver(properties, this.createChainValidator(properties), entityStatementTree, this.processor,
        this.resolverResponseFactory(properties));
  }

  private ChainValidator createChainValidator(final ResolverProperties properties) {
    return new ChainValidator(List.of(
        new SignatureValidationStep(new JWKSet(properties.trustedKeys())),
        new ConstraintsValidationStep(),
        new CriticalClaimsValidationStep()
    ));
  }

  private ResolverResponseFactory resolverResponseFactory(final ResolverProperties properties) {
    return new ResolverResponseFactory(Clock.systemUTC(), properties, this.signerAdapter);
  }
}
