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
package se.digg.oidfed.service.trustanchor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.EntityRecordRegistry;
import se.digg.oidfed.common.entity.integration.RecordRegistrySource;
import se.digg.oidfed.trustanchor.SubordinateStatementFactory;
import se.digg.oidfed.trustanchor.TrustAnchor;

import java.util.List;

/**
 * Configuration for Trust Anchor.
 *
 * @author Felix Hellman
 */
@Configuration
@ConditionalOnProperty(value = TrustAnchorModuleProperties.PROPERTY_PATH + ".active", havingValue = "true")
@EnableConfigurationProperties(TrustAnchorModuleProperties.class)
public class TrustAnchorConfiguration {

  @Bean
  List<TrustAnchor> trustAnchor(final EntityRecordRegistry registry, final TrustAnchorModuleProperties properties, final
  SubordinateStatementFactory factory) {
    return properties.getAnchors()
        .stream()
        .map(a -> TrustAnchorFactory.create(registry, a, factory))
        .toList();
  }

  @Bean
  SubordinateStatementFactory trustAnchorEntityStatementFactory(final RecordRegistrySource source) {
    return new SubordinateStatementFactory(source);
  }
}
