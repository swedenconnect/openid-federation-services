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

/**
 * Thrown when an Entity Configuration cannot be fetched, carrying the kind of failure.
 *
 * @author Martin Lindström
 */
public class FederationFetchException extends RuntimeException {

  private final FetchError error;

  /**
   * Constructor.
   *
   * @param error the kind of failure
   * @param message the message
   * @param cause the cause, may be null
   */
  public FederationFetchException(final FetchError error, final String message, final Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  /**
   * @return the kind of failure
   */
  public FetchError getError() {
    return this.error;
  }
}
