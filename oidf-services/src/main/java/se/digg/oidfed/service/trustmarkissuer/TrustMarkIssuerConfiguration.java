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
package se.digg.oidfed.service.trustmarkissuer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.service.submodule.InMemorySubModuleRegistry;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkProperties;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for trust mark.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(TrustMarkIssuerConfigurationProperties.class)
@ConditionalOnProperty(value = TrustMarkIssuerConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustMarkIssuerConfiguration {

  @Autowired
  void trustMarkIssuer(TrustMarkIssuerConfigurationProperties properties,
      InMemorySubModuleRegistry inMemorySubModuleRegistry) {
    final List<TrustMarkProperties> trustMarkIssuersProperties =
        Optional.ofNullable(properties.getTrustMarkIssuers())
            .orElseThrow(() -> new IllegalArgumentException("TrustMarkIssuers is empty. Check application properties"));

    final List<TrustMarkIssuer> trustMarkIssuers = trustMarkIssuersProperties.stream()
        .map(TrustMarkIssuer::new).toList();

    inMemorySubModuleRegistry.registerTrustMarkIssuer(trustMarkIssuers);

  }
}
