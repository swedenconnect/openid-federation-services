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
 *  limitations under the License.
 */

package se.digg.oidfed.service.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import se.digg.oidfed.trustmarkissuer.exception.FederationException;

import java.util.Map;

/**
 * Error handling that formats error responses to federarion standards
 *
 * @author Per Fredrik Plars
 */
@ControllerAdvice
public class ErrorHandler {

  /**
   * Handle FederationException
   * @param ex FederationException
   * @return HttpStatusCode loaded from exception, also the jsonstructure
   */
  @ExceptionHandler(FederationException.class)
  public ResponseEntity<Map<String, String>> handleFederationException(FederationException ex) {
    return ResponseEntity.status(ex.httpStatusCode()).body(ex.toJSONObject());
  }

}
