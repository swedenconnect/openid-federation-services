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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;

/**
 * Configuration class for running the service with local configuration ONLY
 *
 * @author Felix Hellman
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    name = "openid.federation.registry.integration.enabled",
    havingValue = "false",
    matchIfMissing = true)

public class LocalConfiguration {
  @Bean
  RecordRegistryIntegration throwingFederationClient() {
    return new ThrowingRecordRegistryIntegration();
  }
}
