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
package se.digg.oidfed.service.entity;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.digg.oidfed.common.entity.EntityProperties;
import se.digg.oidfed.common.entity.EntityRegistry;
import se.digg.oidfed.common.entity.EntityStatementFactory;
import se.digg.oidfed.common.keys.KeyRegistry;

import java.util.List;

/**
 * Configuration for entity registry.
 *
 * @author Felix Hellman
 */
@Configuration
@EnableConfigurationProperties(EntityConfigurationProperties.class)
public class EntityConfiguration {

  @Bean
  EntityRegistry entityRegistry(
      final EntityConfigurationProperties properties,
      final KeyRegistry keyRegistry) {
    final List<EntityProperties> mappedProperties =
        properties.getEntityRegistry()
            .stream()
            .map(p -> p.toEntityProperties(keyRegistry))
            .toList();
    return new EntityRegistry(mappedProperties);
  }

  @Bean
  EntityStatementFactory entityStatementFactory() {
    return new EntityStatementFactory();
  }
}
