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
 */

package se.digg.oidfed.common.exception;

/**
 * The server hosting the federation endpoint is currently unable to handle the request due to a temporary overloading
 * or maintenance. The HTTP response status code SHOULD be 503 (Service Unavailable).
 *
 * @author Per Fredrik Plars
 */
public class TemporaryUnavailableException extends FederationException {
  private final static String ERROR = "temporary_unavailable";
  /**
   * TemporaryUnavailableException
   * @param errorDescription Human readable description
   */
  public TemporaryUnavailableException(final String errorDescription) {
    super(ERROR, errorDescription);
  }
  /**
   * TemporaryUnavailableException
   * @param errorDescription Human readable description
   * @param cause Cause for this exception
   */
  public TemporaryUnavailableException(final String errorDescription, final Throwable cause) {
    super(ERROR, errorDescription, cause);
  }
  /**
   * {@inheritDoc}
   *
   */
  @Override
  public int httpStatusCode() {
    return 503;
  }
}
