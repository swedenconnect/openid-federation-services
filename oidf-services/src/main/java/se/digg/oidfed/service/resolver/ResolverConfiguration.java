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
package se.digg.oidfed.service.resolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.resolver.Discovery;
import se.digg.oidfed.resolver.Resolver;

/**
 * Configuration class for Resolver.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(ResolverConfigurationProperties.class)
@ConditionalOnProperty(value = ResolverConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class ResolverConfiguration {
  @Bean
  Resolver resolver() {
    return new Resolver();
  }

  @Bean
  Discovery discovery() {
    return new Discovery();
  }
}
