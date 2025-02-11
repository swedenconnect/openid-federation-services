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
package se.digg.oidfed.service.trustanchor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.registry.RefreshAheadRecordRegistrySource;
import se.digg.oidfed.common.jwt.SignerFactory;

/**
 * Configuration for Trust Anchor.
 *
 * @author Felix Hellman
 */
@Configuration
public class TrustAnchorConfiguration {
  @Bean
  TrustAnchorFactory trustAnchorFactory(
      final RefreshAheadRecordRegistrySource source,
      final EntityRecordRegistry registry,
      final SignerFactory signerFactory,
      final FederationClient client) {
    return new TrustAnchorFactory(registry, source, signerFactory, client);
  }
}
