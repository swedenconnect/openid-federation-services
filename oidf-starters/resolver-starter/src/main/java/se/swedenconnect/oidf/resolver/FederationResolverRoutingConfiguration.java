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
package se.swedenconnect.oidf.resolver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.swedenconnect.oidf.resolver.routing.ResolverRouter;
import se.swedenconnect.oidf.routing.RouteFactory;
import se.swedenconnect.oidf.routing.ServerResponseErrorHandler;

/**
 * Routing Configuration for resolver.
 *
 * @author Felix Hellman
 */
@Configuration
public class FederationResolverRoutingConfiguration {
  @Bean
  ResolverRouter resolverRouter(
      final ResolverFactory factory,
      final RouteFactory routeFactory,
      final ServerResponseErrorHandler errorHandler
      ) {
    return new ResolverRouter(factory, routeFactory, errorHandler);
  }
}
