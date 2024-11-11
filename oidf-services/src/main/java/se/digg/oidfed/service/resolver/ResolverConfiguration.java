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
import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.resolver.Discovery;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.resolver.ResolverProperties;
import se.digg.oidfed.resolver.ResolverResponseFactory;
import se.digg.oidfed.resolver.chain.ChainValidator;
import se.digg.oidfed.resolver.chain.ConstraintsValidationStep;
import se.digg.oidfed.resolver.chain.CriticalClaimsValidationStep;
import se.digg.oidfed.resolver.chain.SignatureValidationStep;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.metadata.OIDFPolicyOperationFactory;
import se.digg.oidfed.resolver.tree.EntityStatementTree;
import se.digg.oidfed.resolver.tree.EntityStatementTreeLoader;
import se.digg.oidfed.resolver.tree.Tree;
import se.digg.oidfed.resolver.tree.VersionedCacheLayer;
import se.digg.oidfed.resolver.tree.resolution.BFSExecution;
import se.digg.oidfed.resolver.tree.resolution.DefaultErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;
import se.digg.oidfed.resolver.tree.resolution.ScheduledStepRecoveryStrategy;
import se.digg.oidfed.service.resolver.observability.ObservableErrorContext;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Configuration class for Resolver.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(ResolverConfigurationProperties.class)
@ConditionalOnProperty(value = ResolverConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
@Slf4j
public class ResolverConfiguration {
  @Bean
  Resolver resolver(final ResolverProperties resolverProperties,
                    final ChainValidator validator,
                    final EntityStatementTree tree,
                    final MetadataProcessor processor,
                    final ResolverResponseFactory factory) {
    return new Resolver(resolverProperties, validator, tree, processor, factory);
  }

  @Bean
  ResolverProperties resolverProperties(final ResolverConfigurationProperties properties) {
    return properties.toResolverProperties();
  }

  @Bean
  ChainValidator chainValidator(final ResolverProperties properties) {
    return new ChainValidator(
        List.of(
            new SignatureValidationStep(new JWKSet(properties.trustedKeys())),
            new ConstraintsValidationStep(),
            new CriticalClaimsValidationStep())
    );
  }

  @Bean
  EntityStatementTree entityStatementTree(final EntityStatementTreeLoader loader, final ResolverProperties properties,
                                          final Tree<EntityStatement> tree) {
    final EntityStatementTree entityStatementTree = new EntityStatementTree(tree);
    entityStatementTree.load(loader, "%s/.well-known/openid-federation".formatted(properties.trustAnchor()));
    return entityStatementTree;
  }

  @Bean
  Tree<EntityStatement> internalTree(final RedisVersionedCacheLayer redisVersionedCacheLayer) {
    return new Tree<>(redisVersionedCacheLayer);
  }

  @Bean
  EntityStatementTreeLoader loader(final EntityStatementIntegration integration,
                                   final ExecutionStrategy executionStrategy,
                                   final VersionedCacheLayer<EntityStatement> versionedCacheLayer,
                                   final ResolverProperties properties, final ErrorContextFactory errorContextFactory) {

    return new EntityStatementTreeLoader(integration, executionStrategy,
        new ScheduledStepRecoveryStrategy(Executors.newSingleThreadScheduledExecutor(), properties),
        errorContextFactory)
        .withAdditionalPostHook(versionedCacheLayer::useNextVersion);
  }

  @Bean
  ExecutionStrategy resolutionStrategy() {
    return new BFSExecution(Executors.newSingleThreadExecutor());
  }

  @Bean
  EntityStatementIntegration integration() {
    return new RestClientEntityStatementIntegration();
  }

  @Bean
  MetadataProcessor metadataProcessor() {
    return new MetadataProcessor(new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator());
  }

  @Bean
  ResolverResponseFactory resolverResponseFactory(final ResolverProperties properties) {
    return new ResolverResponseFactory(Clock.systemUTC(), properties);
  }

  @Bean
  Discovery discovery(final EntityStatementTree tree) {
    return new Discovery(tree);
  }

  @Bean
  RedisVersionedCacheLayer redisDataLayer(
      final RedisTemplate<String, Integer> versionTemplate,
      final RedisOperations redisOperations
  ) {
    return new RedisVersionedCacheLayer(versionTemplate, redisOperations);
  }

  @Bean
  RedisOperations redisOperations(
      final RedisTemplate<String, EntityStatement> template,
      final RedisTemplate<String, String> childrenTemplate
  ) {
    return new RedisOperations(template, childrenTemplate);
  }

  @Bean
  RedisTemplate<String, EntityStatement> redisTemplate(final RedisConnectionFactory factory) {
    RedisTemplate<String, EntityStatement> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setValueSerializer(new EntityStatementSerializer());
    return template;
  }

  @Bean
  RedisTemplate<String, Integer> integerRedisTemplate(final RedisConnectionFactory factory) {
    RedisTemplate<String, Integer> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    return template;
  }

  @Bean
  ErrorContextFactory errorContextFactory(final MeterRegistry registry) {
    final DefaultErrorContextFactory factory = new DefaultErrorContextFactory();
    return location -> {
      final Counter counter = registry.counter(
          "resovler_tree_step_failure",
          List.of(Tag.of("location", location))
      );
      return new ObservableErrorContext(factory.create(location), counter);
    };
  }

  @Bean
  RestClient resolverClient(final SslBundles bundles, final ResolverConfigurationProperties properties) {
    final HttpClient.Builder builder = HttpClient.newBuilder();

    Optional.ofNullable(properties.getTrustStoreBundle())
        .ifPresentOrElse(bundleName -> {
              final SslBundle bundle = bundles.getBundle(bundleName);
              builder.sslContext(bundle.createSslContext());
            },
            () -> log.info("Resolver was started without a trust-store, using default ..."));

    return RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory(builder.build()))
        .build();
  }

  @Bean
  MeterRegistry registry() {
    return new CompositeMeterRegistry(io.micrometer.core.instrument.Clock.SYSTEM);
  }
}
