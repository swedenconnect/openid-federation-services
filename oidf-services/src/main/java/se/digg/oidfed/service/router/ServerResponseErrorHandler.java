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

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;
import se.digg.oidfed.common.exception.FederationException;
import se.digg.oidfed.service.error.ErrorHandler;

import java.util.Map;

@Component
public class ServerResponseErrorHandler {
  private final ErrorHandler errorHandler;

  public ServerResponseErrorHandler(final ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public ServerResponse handle(final FederationException e) {
    final ResponseEntity<Map<String, String>> error = this.errorHandler.handleFederationException(e);
    final Map<String, String> body = error.getBody();
    return ServerResponse.status(error.getStatusCode()).body(body);
  }
}
