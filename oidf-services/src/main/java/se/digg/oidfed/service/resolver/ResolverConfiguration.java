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

import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.resolver.metadata.MetadataProcessor;
import se.digg.oidfed.resolver.metadata.OIDFPolicyOperationFactory;
import se.digg.oidfed.resolver.tree.resolution.DFSExecution;
import se.digg.oidfed.resolver.tree.resolution.ErrorContextFactory;
import se.digg.oidfed.resolver.tree.resolution.ExecutionStrategy;
import se.digg.oidfed.service.resolver.cache.CompositeTreeLoader;
import se.digg.oidfed.service.resolver.cache.ResolverCacheFactory;
import se.digg.oidfed.service.resolver.cache.ResolverCacheRegistry;
import se.digg.oidfed.service.resolver.observability.ObservableErrorContextFactory;

/**
 * Configuration class for Resolver.
 *
 * @author Felix Hellman
 */
@Configuration
@Slf4j
public class ResolverConfiguration {
  @Bean
  ExecutionStrategy resolutionStrategy() {
    return new DFSExecution();
  }


  @Bean
  MetadataProcessor metadataProcessor() {
    return new MetadataProcessor(new OIDFPolicyOperationFactory(), new DefaultPolicyOperationCombinationValidator());
  }

  @Bean
  ResolverFactory resolverFactory(
      final ResolverCacheFactory factory,
      final MetadataProcessor processor,
      final EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory,
      final ResolverCacheRegistry registry,
      final SignerFactory adapter
  ) {
    return new ResolverFactory(factory, processor, entityStatementTreeLoaderFactory, registry, adapter);
  }

  @Bean
  ResolverCacheRegistry cacheRegistry() {
    return new ResolverCacheRegistry();
  }

  @Bean
  CompositeTreeLoader compositeTreeLoader(final ResolverCacheRegistry registry, final ResolverFactory factory,
                                          final CompositeRecordSource source) {
    return new CompositeTreeLoader(registry, factory, source);
  }

  @Bean
  EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory(
      final FederationClient client,
      final ExecutionStrategy strategy,
      final ErrorContextFactory factory,
      final ApplicationEventPublisher publisher
  ) {
    return new EntityStatementTreeLoaderFactory(client, strategy, factory, publisher);
  }

  @Bean
  ErrorContextFactory errorContextFactory(final MeterRegistry registry) {
    return new ObservableErrorContextFactory(registry);
  }
}
