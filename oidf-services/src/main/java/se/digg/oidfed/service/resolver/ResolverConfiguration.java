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
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.resolver.ResolverProperties;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.metadata.OIDFPolicyOperationFactory;
import se.digg.oidfed.resolver.tree.resolution.BFSExecution;
import se.digg.oidfed.resolver.tree.resolution.DefaultErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;
import se.digg.oidfed.service.resolver.observability.ObservableErrorContext;

import java.net.http.HttpClient;
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
  List<Resolver> resolvers(final List<ResolverProperties> resolverProperties,
      final ResolverFactory resolverFactory
  ) {
    return resolverProperties.stream().map(resolverFactory::create).toList();
  }

  @Bean
  List<ResolverProperties> resolverProperties(final ResolverConfigurationProperties properties) {
    return properties.getResolvers().stream().map(
        ResolverConfigurationProperties.ResolverModuleProperties::toResolverProperties).toList();
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
  ResolverFactory resolverFactory(
      final RedisTemplate<String, Integer> versionTemplate,
      final RedisOperations redisOperations,
      final MetadataProcessor processor,
      final EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory
  ) {
    return new ResolverFactory(versionTemplate, redisOperations, processor, entityStatementTreeLoaderFactory);
  }

  @Bean
  EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory(
      final EntityStatementIntegration integration,
      final ExecutionStrategy strategy,
      final ErrorContextFactory factory) {
    return new EntityStatementTreeLoaderFactory(integration, strategy, factory);
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
