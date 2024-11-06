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
 * The server encountered an unexpected condition that prevented it from fulfilling the request. The HTTP response
 * status code SHOULD be one in the 5xx range, like 500 (Internal Server Error).
 *
 * @author Per Fredrik Plars
 */
public class ServerErrorException extends FederationException {
  private final static String ERROR = "server_error";

  public ServerErrorException(final String errorDescription) {
    super(ERROR, errorDescription);
  }

  public ServerErrorException(final String errorDescription, final Throwable cause) {
    super(ERROR, errorDescription, cause);
  }
}
