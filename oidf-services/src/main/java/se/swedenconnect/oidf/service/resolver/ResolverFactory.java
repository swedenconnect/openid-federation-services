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
package se.swedenconnect.oidf.service.resolver;

import com.nimbusds.jose.jwk.JWKSet;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.common.entity.tree.ResolverCache;
import se.swedenconnect.oidf.common.entity.tree.Tree;
import se.swedenconnect.oidf.resolver.Resolver;
import se.swedenconnect.oidf.resolver.ResolverResponseFactory;
import se.swedenconnect.oidf.resolver.ValidatingResolver;
import se.swedenconnect.oidf.resolver.chain.ChainValidator;
import se.swedenconnect.oidf.resolver.chain.ConstraintsValidationStep;
import se.swedenconnect.oidf.resolver.chain.CriticalClaimsValidationStep;
import se.swedenconnect.oidf.resolver.chain.SignatureValidationStep;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTree;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheFactory;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheRegistration;
import se.swedenconnect.oidf.service.resolver.cache.ResolverCacheRegistry;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;

/**
 * Factory class for creating {@link ValidatingResolver} from {@link ResolverProperties}
 *
 * @author Felix Hellman
 */
public class ResolverFactory {

  private final ResolverCacheFactory resolverCacheFactory;
  private final MetadataProcessor processor;
  private final EntityStatementTreeLoaderFactory treeLoaderFactory;
  private final ResolverCacheRegistry registry;
  private final SignerFactory signerFactory;
  private final List<Function<Resolver, Resolver>> transformers;

  /**
   * Constructor.
   *
   * @param resolverCacheFactory factory for creating snap resolver caches
   * @param processor            to use for metadata
   * @param treeLoaderFactory    to use for creating tree loaders
   * @param registry             for caches
   * @param signerFactory        to use
   * @param transformers         functions to apply on resolver
   */
  public ResolverFactory(
      final ResolverCacheFactory resolverCacheFactory,
      final MetadataProcessor processor,
      final EntityStatementTreeLoaderFactory treeLoaderFactory,
      final ResolverCacheRegistry registry,
      final SignerFactory signerFactory,
      final List<Function<Resolver, Resolver>> transformers) {

    this.resolverCacheFactory = resolverCacheFactory;
    this.processor = processor;
    this.treeLoaderFactory = treeLoaderFactory;
    this.registry = registry;
    this.signerFactory = signerFactory;
    this.transformers = transformers;
  }


  /**
   * Creates a new instance of a {@link ValidatingResolver}
   *
   * @param properties to create a module from
   * @return new instance
   */
  public Resolver create(final ResolverProperties properties) {
    if (this.registry.getRegistration(properties.entityIdentifier()).isEmpty()) {
      final ResolverCache entityStatementSnapshotSource = this.resolverCacheFactory.create(properties);
      final EntityStatementTree entityStatementTree =
          new EntityStatementTree(new Tree<>(entityStatementSnapshotSource));
      this.registerCache(properties, entityStatementTree, entityStatementSnapshotSource);
    }

    final ResolverCacheRegistration registration = this.registry.getRegistration(properties.entityIdentifier()).get();

    final ValidatingResolver resolver = new ValidatingResolver(
        properties,
        this.createChainValidator(properties),
        registration.tree(),
        this.processor,
        this.resolverResponseFactory(properties)
    );

    return this.transformers.stream()
        .reduce(t -> t, Function::andThen)
        .apply(resolver);
  }

  private void registerCache(
      final ResolverProperties properties,
      final EntityStatementTree entityStatementTree,
      final ResolverCache entityStatementSnapshotSource) {
    this.registry.registerCache(properties.entityIdentifier(), new ResolverCacheRegistration(
        entityStatementTree,
        this.treeLoaderFactory.create(properties),
        entityStatementSnapshotSource,
        properties
    ));
  }

  private ChainValidator createChainValidator(final ResolverProperties properties) {
    return new ChainValidator(List.of(
        new SignatureValidationStep(new JWKSet(properties.trustedKeys())),
        new ConstraintsValidationStep(),
        new CriticalClaimsValidationStep()
    ));
  }

  private ResolverResponseFactory resolverResponseFactory(final ResolverProperties properties) {
    return new ResolverResponseFactory(Clock.systemUTC(), properties, this.signerFactory);
  }
}
