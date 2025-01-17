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
package se.digg.oidfed.service.submodule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.jwt.SignerFactory;
import se.digg.oidfed.common.keys.KeyRegistry;
import se.digg.oidfed.resolver.Resolver;
import se.digg.oidfed.service.configuration.OpenIdFederationConfigurationProperties;
import se.digg.oidfed.service.keys.FederationKeyConfigurationProperties;
import se.digg.oidfed.service.keys.FederationKeys;
import se.digg.oidfed.service.modules.SubModuleVerifier;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerFactory;
import se.digg.oidfed.trustanchor.TrustAnchor;
import se.digg.oidfed.trustmarkissuer.InMemoryTrustMarkSubjectRepository;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkSigner;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRepository;

import java.time.Clock;
import java.util.List;

/**
 * Configuration for submodules.
 *
 * @author Felix Hellman
 */
@Configuration
public class SubmoduleConfiguration {
  @Bean
  InMemorySubModuleRegistry inMemorySubModuleRegistry(
      final List<Resolver> resolvers,
      final List<TrustAnchor> trustAnchors,
      final List<TrustMarkIssuer> trustMarkIssuers) {
    final InMemorySubModuleRegistry inMemorySubModuleRegistry = new InMemorySubModuleRegistry();
    inMemorySubModuleRegistry.registerResolvers(resolvers);
    inMemorySubModuleRegistry.registerTrustAnchor(trustAnchors);
    inMemorySubModuleRegistry.registerTrustMarkIssuer(trustMarkIssuers);
    return inMemorySubModuleRegistry;
  }


  @Bean
  SubModuleVerifier subModuleVerifier(final FederationKeys keys) {
    return new SubModuleVerifier(keys.validationKeys());
  }

  @Bean
  TrustMarkIssuerFactory factory(
      final TrustMarkSigner signer,
      final TrustMarkSubjectRepository repository
  ) {
    return new TrustMarkIssuerFactory(signer, repository);
  }

  @Bean
  TrustMarkSigner trustMarkSigner(final SignerFactory adapter, final Clock clock) {
    return new TrustMarkSigner(adapter, clock);
  }

  @Bean
  SignerFactory entityToSignerAdapter(final FederationKeys keys) {
    return new SignerFactory(keys.signKeys());
  }

  @Bean
  TrustMarkSubjectRepository trustMarkSubjectRepository(final Clock clock) {
    return new InMemoryTrustMarkSubjectRepository(clock);
  }

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
