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
package se.swedenconnect.oidf.service.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.CacheFactory;
import se.swedenconnect.oidf.FederationKeys;
import se.swedenconnect.oidf.OpenIdFederationProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.LocalRegistryProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryRefreshAheadCache;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.keys.KeyRegistry;
import se.swedenconnect.oidf.resolver.ResolverCacheRegistry;
import se.swedenconnect.oidf.resolver.ResolverFactory;
import se.swedenconnect.oidf.service.entity.PolicyConfigurationProperties;
import se.swedenconnect.oidf.service.resolver.cache.CompositeTreeLoader;
import se.swedenconnect.oidf.service.trustanchor.TrustAnchorModuleProperties;
import se.swedenconnect.oidf.service.trustmarkissuer.TrustMarkIssuerModuleConfigurationProperties;
import se.swedenconnect.oidf.service.trustmarkissuer.TrustMarkSubjectProperties;

import java.util.List;
import java.util.Optional;

/**
 * Configuration class for openid federation.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(OpenIdFederationServiceProperties.class)
public class OpenIdFederationConfiguration {

  @Bean
  RegistryRefreshAheadCache refreshAheadCache(final CacheFactory factory) {
    return new RegistryRefreshAheadCache(
        factory.create(ModuleRecord.class),
        factory.createListValueCache(TrustMarkSubjectRecord.class),
        factory.createListValueCache(EntityRecord.class),
        factory.create(PolicyRecord.class)
    );
  }

  @Bean
  CompositeTreeLoader compositeTreeLoader(final ResolverCacheRegistry resolverCacheRegistry,
                                          final ResolverFactory resolverFactory,
                                          final CompositeRecordSource recordSource) {
    return new CompositeTreeLoader(resolverCacheRegistry, resolverFactory, recordSource);
  }
}
