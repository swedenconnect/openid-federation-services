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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.service.keys.FederationKeys;
import se.digg.oidfed.trustmarkissuer.TrustMarkSubjectRecordVerifier;

/**
 * Configuration for trust mark.
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TrustMarkIssuerModuleProperties.class)
@ConditionalOnProperty(value = TrustMarkIssuerModuleProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustMarkIssuerConfiguration {
  @Bean
  RestClientTrustMarkSubjectRecordIntegration restClientTrustMarkSubjectRecordIntegration(@Qualifier("entity-record" +
      "-integration-client") final RestClient client, final TrustMarkSubjectRecordVerifier verifier) {
    return new RestClientTrustMarkSubjectRecordIntegration(client, verifier);
  }

  @Bean
  TrustMarkSubjectRecordVerifier trustMarkSubjectRecordVerifier(final FederationKeys keys) {
    return new TrustMarkSubjectRecordVerifier(keys.validationKeys());
  }
}
