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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.data.redis.core.RedisTemplate;
import se.digg.oidfed.common.tree.Tree;
import se.digg.oidfed.common.tree.VersionedCacheLayer;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.resolver.ResolverProperties;
import se.digg.oidfed.resolver.ResolverResponseFactory;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.chain.ConstraintsValidationStep;
import se.digg.oidfed.resolver.chain.CriticalClaimsValidationStep;
import se.digg.oidfed.resolver.chain.SignatureValidationStep;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.tree.EntityStatementTree;

import java.time.Clock;
import java.util.List;

/**
 * Factory class for creating {@link Resolver} from {@link ResolverProperties}
 *
 * @author Felix Hellman
 */
public class ResolverFactory {

  private final RedisTemplate<String, Integer> versionTemplate;
  private final RedisOperations redisOperations;
  private final MetadataProcessor processor;
  private final EntityStatementTreeLoaderFactory treeLoaderFactory;

  /**
   * Constructor.
   * @param versionTemplate to use for cache
   * @param redisOperations to use for cache
   * @param processor to use for metadata
   * @param treeLoaderFactory to use for creating tree loaders
   */
  public ResolverFactory(final RedisTemplate<String, Integer> versionTemplate, final RedisOperations redisOperations,
      final MetadataProcessor processor, final EntityStatementTreeLoaderFactory treeLoaderFactory) {
    this.versionTemplate = versionTemplate;
    this.redisOperations = redisOperations;
    this.processor = processor;
    this.treeLoaderFactory = treeLoaderFactory;
  }

  /**
   * Creates a new instance of a {@link Resolver}
   * @param properties to create a module from
   * @return new instance
   */
  public Resolver create(final ResolverProperties properties) {
    final RedisVersionedCacheLayer redisVersionedCacheLayer =
        new RedisVersionedCacheLayer(versionTemplate, redisOperations, properties);
    return new Resolver(properties, createChainValidator(properties), entityStatementTree(properties,
        createTree(redisVersionedCacheLayer), redisVersionedCacheLayer), processor,
        resolverResponseFactory(properties));
  }

  private Tree<EntityStatement> createTree(final RedisVersionedCacheLayer redisVersionedCacheLayer) {
    return new Tree<>(redisVersionedCacheLayer);
  }

  private ChainValidator createChainValidator(final ResolverProperties properties) {
    return new ChainValidator(List.of(
        new SignatureValidationStep(new JWKSet(properties.trustedKeys())),
        new ConstraintsValidationStep(),
        new CriticalClaimsValidationStep()
    ));
  }

  private EntityStatementTree entityStatementTree(
      final ResolverProperties properties,
      final Tree<EntityStatement> tree, final VersionedCacheLayer<EntityStatement> versionedCacheLayer) {
    final EntityStatementTree entityStatementTree = new EntityStatementTree(tree);
    entityStatementTree.load(treeLoaderFactory.create(versionedCacheLayer, properties),
        "%s/.well-known/openid-federation".formatted(properties.trustAnchor()));
    return entityStatementTree;
  }

  private ResolverResponseFactory resolverResponseFactory(final ResolverProperties properties) {
    return new ResolverResponseFactory(Clock.systemUTC(), properties);
  }
}
