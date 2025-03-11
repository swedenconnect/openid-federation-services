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
package se.digg.oidfed.service.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import se.digg.oidfed.common.entity.integration.CompositeRecordSource;

import java.util.List;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router for serving federation content.
 *
 * @author Felix Hellman
 */
@Component
@Slf4j
public class MainRouter {


  private final CompositeRecordSource source;
  private final RouterFunctionMapping mapping;
  private final List<Router> routers;

  public MainRouter(final CompositeRecordSource source, final RouterFunctionMapping mapping, final List<Router> routers) {
    this.source = source;
    this.mapping = mapping;
    this.routers = routers;
  }

  /**
   * Calculates and updates endpoints for entity-configurations.
   */
  public void reEvaluateEndpoints() {
    final RouterFunctions.Builder route = route();
    this.routers.forEach(router -> router.evaluateEndpoints(this.source, route));
    final RouterFunction<ServerResponse> functions = route.build();
    this.mapping.setRouterFunction(functions);
    RouterFunctions.changeParser(functions, this.mapping.getPatternParser());
  }
}
