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

package se.swedenconnect.oidf.common.entity.exception;


/**
 * The requested Entity Identifier is "not found". The HTTP response status code SHOULD be 404 (Not Found).
 *
 * @author Per Fredrik Plars
 */
public class NotFoundException extends FederationException {
  private final static String ERROR = "not_found";

  /**
   * NotFoundException
   * @param errorDescription Human readable description
   */
  public NotFoundException(final String errorDescription) {
    super(ERROR, errorDescription);
  }
  /**
   * NotFoundException
   * @param errorDescription Human readable description
   * @param cause Cause for this exception
   */
  public NotFoundException(final String errorDescription, final Throwable cause) {
    super(ERROR, errorDescription, cause);
  }
  /**
   * {@inheritDoc}
   *
   */
  @Override
  public int httpStatusCode() {
    return 404;
  }
}
