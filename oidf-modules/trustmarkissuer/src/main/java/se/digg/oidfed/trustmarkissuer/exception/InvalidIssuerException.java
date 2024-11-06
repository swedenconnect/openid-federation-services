/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer.exception;

/**
 * The endpoint cannot service the requested issuer. The HTTP response status code SHOULD be 404 (Not Found).
 *
 * @author Per Fredrik Plars
 */
public class InvalidIssuerException extends FederationException {
  private final static String ERROR = "invalid_issuer";

  public InvalidIssuerException(final String errorDescription) {
    super(ERROR, errorDescription);
  }

  public InvalidIssuerException(final String errorDescription, final Throwable cause) {
    super(ERROR, errorDescription, cause);
  }

  @Override
  public int httpStatusCode() {
    return 404;
  }
}
