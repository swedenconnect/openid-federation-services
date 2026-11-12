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
package se.swedenconnect.oidf.common.entity.exception;

/**
 * The endpoint is temporarily unable to handle the request (OpenID Federation 1.0, Section 8.9). The HTTP response
 * status code is 503 (Service Unavailable).
 *
 * @author Per Fredrik Plars
 */
public class TemporarilyUnavailableException extends FederationException {

  /** The error code. */
  private static final String ERROR = "temporarily_unavailable";

  /**
   * Constructor.
   *
   * @param errorDescription human readable description
   */
  public TemporarilyUnavailableException(final String errorDescription) {
    super(ERROR, errorDescription);
  }

  /**
   * Constructor.
   *
   * @param errorDescription human readable description
   * @param cause cause for this exception
   */
  public TemporarilyUnavailableException(final String errorDescription, final Throwable cause) {
    super(ERROR, errorDescription, cause);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int httpStatusCode() {
    return 503;
  }
}
