/*
 * Copyright 2024-2026 Sweden Connect
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
package se.swedenconnect.oidf.routing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests for the error codes of {@link ErrorHandler}.
 *
 * @author Martin Lindström
 */
class ErrorHandlerTest {

  @Test
  void httpStatusesMapToSection89Codes() {
    Assertions.assertEquals("invalid_request", ErrorHandler.errorCode(HttpStatus.BAD_REQUEST));
    Assertions.assertEquals("invalid_request", ErrorHandler.errorCode(HttpStatus.METHOD_NOT_ALLOWED));
    Assertions.assertEquals("invalid_request", ErrorHandler.errorCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    Assertions.assertEquals("invalid_client", ErrorHandler.errorCode(HttpStatus.UNAUTHORIZED));
    Assertions.assertEquals("not_found", ErrorHandler.errorCode(HttpStatus.NOT_FOUND));
    Assertions.assertEquals("server_error", ErrorHandler.errorCode(HttpStatus.INTERNAL_SERVER_ERROR));
    Assertions.assertEquals("temporarily_unavailable", ErrorHandler.errorCode(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
