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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.entity.EntityRegistry;
import se.digg.oidfed.common.entity.EntityStatementFactory;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router for serving /.well-known/openid-federation entity configurations
 *
 * @author Felix Hellman
 */
@Configuration
public class EntityRouter {

  /**
   * @param registry to handle alias mapping
   * @param factory to construct entity configurations
   * @return router function bean
   */
  @Bean
  public RouterFunction<ServerResponse> entityConfigurationRouterFunction(final EntityRegistry registry, final
      EntityStatementFactory factory) {

    final RouterFunctions.Builder route = route();

    route.GET("/.well-known/openid-federation", r -> {
      return ServerResponse.ok().body(
          registry.getEntity("/")
              .map(factory::createEntityConfiguration)
              .map(e -> e.getSignedStatement().serialize())
              .orElseThrow());
    });

    registry.getPaths().forEach(path -> {
      route.GET("%s/.well-known/openid-federation".formatted(path), r -> {
        return ServerResponse.ok().body(
            registry.getEntity(path)
                .map(factory::createEntityConfiguration)
                .map(e -> e.getSignedStatement().serialize())
                .orElseThrow());
      });
    });

    return route.build();
  }

}
