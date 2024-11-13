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

import java.util.Map;

/**
 * Federation Error Response defines in specification. https://openid.net/specs/openid-federation-1_0.html#section-8.9
 *
 * Complemented https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#extensions-error
 *
 * @author Per Fredrik Plars
 */
public class FederationException extends Exception {
  private final String error;
  private final String errorDescription;

  /**
   * General federation exception
   * @param error Error according to specification
   * @param errorDescription Error description that make sense for human to read
   */
  public FederationException(final String error, final String errorDescription) {
    super(String.format("Error:%s Description:%s", error, errorDescription));
    this.error = error;
    this.errorDescription = errorDescription;
  }
  /**
   * General federation exception
   * @param error Error according to specification
   * @param errorDescription Error description that make sense for human to read
   * @param cause Cause for this exception
   */
  public FederationException(final String error, final String errorDescription, final Throwable cause) {
    super(String.format("Error:%s Description:%s", error, errorDescription), cause);
    this.error = error;
    this.errorDescription = errorDescription;
  }

  /**
   * JsonObject structure
   * @return Map of string,string
   */
  public Map<String, String> toJSONObject() {
    return Map.of("error", error, "error_description", errorDescription);
  }

  /**
   * HttpStatusCode for this exception
   * @return HttpStatusCode
   */
  public int httpStatusCode() {
    return 500;
  }

}
