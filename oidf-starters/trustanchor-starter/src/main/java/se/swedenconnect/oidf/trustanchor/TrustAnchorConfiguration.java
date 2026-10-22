/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.trustanchor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.jwt.SignerFactory;

import java.time.Duration;

/**
 * Configuration class for TrustAnchor.
 *
 * @author Felix Hellman
 */
@Configuration
public class TrustAnchorConfiguration {
  /**
   * The longest a filtered subordinate listing may spend on fetching the Entity Configurations of the subordinates.
   * Apache HttpClient bounds each request on its own but not a sequence of them, so without a budget the endpoint
   * takes as long as the number of unreachable subordinates allows.
   */
  @Value("${federation.trust-anchor.subordinate-listing-budget:PT10S}")
  private Duration subordinateListingBudget;

  @Bean
  TrustAnchorFactory trustAnchorFactory(
      final CompositeRecordSource recordSource,
      final SignerFactory signerFactory,
      @Qualifier("federationRequestClient") final FederationClient client
  ) {
    return new TrustAnchorFactory(recordSource, signerFactory, client, this.subordinateListingBudget);
  }
}
