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
package se.swedenconnect.oidf.resolver;

import com.nimbusds.openid.connect.sdk.federation.policy.operations.DefaultPolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationCombinationValidator;
import com.nimbusds.openid.connect.sdk.federation.policy.operations.PolicyOperationFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;
import se.swedenconnect.oidf.resolver.metadata.MetadataProcessor;
import se.swedenconnect.oidf.resolver.metadata.OIDFPolicyOperationFactory;
import se.swedenconnect.oidf.resolver.tree.resolution.DFSExecution;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Configuration for Resolver.
 *
 * @author Felix Hellman
 */
@Configuration
public class FederationResolverConfiguration {

  @Bean
  @ConditionalOnMissingBean
  ResolverFactory resolverFactory(
      final ResolverCacheFactory cacheFactory,
      final MetadataProcessor processor,
      final EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory,
      final ResolverCacheRegistry resolverCacheRegistry,
      final SignerFactory signerFactory,
      final List<Function<Resolver, Resolver>> transformers,
      final CompositeRecordSource compositeRecordSource
      ) {
    return new ResolverFactory(
        cacheFactory,
        processor,
        entityStatementTreeLoaderFactory,
        resolverCacheRegistry,
        signerFactory,
        transformers,
        compositeRecordSource
    );
  }

  @Bean
  @ConditionalOnMissingBean
  ResolverCacheRegistry resolverCacheRegistry() {
    return new ResolverCacheRegistry();
  }

  @Bean
  @ConditionalOnMissingBean
  ResolverCacheFactory inMemoryResolverCacheFactory() {
    return new InMemoryResolverCacheFactory();
  }

  @Bean
  @ConditionalOnMissingBean
  MetadataProcessor metadataProcessor(final PolicyOperationFactory policyOperationFactory,
                                      final PolicyOperationCombinationValidator policyOperationCombinationValidator
  ) {
    return new MetadataProcessor(policyOperationFactory, policyOperationCombinationValidator);
  }

  @Bean
  @ConditionalOnMissingBean
  PolicyOperationFactory policyOperationFactory() {
    return new OIDFPolicyOperationFactory();
  }

  @Bean
  @ConditionalOnMissingBean
  PolicyOperationCombinationValidator policyOperationCombinationValidator() {
    return new DefaultPolicyOperationCombinationValidator();
  }

  @Bean
  @ConditionalOnMissingBean
  EntityStatementTreeLoaderFactory entityStatementTreeLoaderFactory(
      final FederationClient client,
      final ErrorContextFactory errorContextFactory,
      final ApplicationEventPublisher publisher) {
    return new EntityStatementTreeLoaderFactory(client, new DFSExecution(), errorContextFactory, publisher);
  }

  @Bean
  @ConditionalOnMissingBean
  ErrorContextFactory errorContextFactory(final MeterRegistry registry) {
    return new ObservableErrorContextFactory(registry);
  }

  @Bean
  @ConditionalOnMissingBean
  CacheEventListener cacheEventListener(final ResolverCacheRegistry registry) {
    return new CacheEventListener(registry);
  }
}
