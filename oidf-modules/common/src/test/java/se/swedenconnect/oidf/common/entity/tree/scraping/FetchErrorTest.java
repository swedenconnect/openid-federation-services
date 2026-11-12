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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.text.ParseException;

/**
 * Tests for {@link FetchError}.
 *
 * @author Martin Lindström
 */
class FetchErrorTest {

  @Test
  void kindIsFoundAmongCauses() {
    Assertions.assertEquals(FetchError.httpStatus(503), FetchError.of(new RuntimeException(
        new FederationFetchException(FetchError.httpStatus(503), "503", null))));
    Assertions.assertEquals(FetchError.Kind.TIMEOUT,
        FetchError.of(new RuntimeException(new SocketTimeoutException("read timed out"))).kind());
    Assertions.assertEquals(FetchError.Kind.CONNECTION_ERROR,
        FetchError.of(new RuntimeException(new ConnectException("refused"))).kind());
    Assertions.assertEquals(FetchError.Kind.INVALID_RESPONSE,
        FetchError.of(new RuntimeException(new ParseException("bad", 0))).kind());
  }

  @Test
  void toStringGivesKindAndStatus() {
    Assertions.assertEquals("http_status 404", FetchError.httpStatus(404).toString());
    Assertions.assertEquals("timeout", new FetchError(FetchError.Kind.TIMEOUT, null).toString());
  }
}
