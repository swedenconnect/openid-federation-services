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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * The kind of problem with the latest fetch of an Entity Configuration: a failed fetch, or a fetched Entity
 * Configuration that has expired.
 *
 * @param kind the kind of failure
 * @param httpStatus the HTTP status of the response, only set for {@link Kind#HTTP_STATUS}
 * @author Martin Lindström
 */
public record FetchError(Kind kind, Integer httpStatus) {

  /**
   * Kinds of failures.
   */
  public enum Kind {
    /** The response had a non-2xx status. */
    HTTP_STATUS,
    /** The request timed out. */
    TIMEOUT,
    /** No connection could be made. */
    CONNECTION_ERROR,
    /** The response could not be parsed. */
    INVALID_RESPONSE,
    /** The Entity Configuration has expired. */
    EXPIRED
  }

  /**
   * Creates an error for a non-2xx response.
   *
   * @param status the HTTP status
   * @return the error
   */
  public static FetchError httpStatus(final int status) {
    return new FetchError(Kind.HTTP_STATUS, status);
  }

  /**
   * Creates an error for an Entity Configuration that has expired.
   *
   * @return the error
   */
  public static FetchError expired() {
    return new FetchError(Kind.EXPIRED, null);
  }

  /**
   * Finds the kind of failure from an exception. A {@link FederationFetchException} gives its own error, timeouts and
   * other I/O errors are found among the causes, and anything else is treated as an invalid response.
   *
   * @param e the exception
   * @return the error
   */
  public static FetchError of(final Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof final FederationFetchException f) {
        return f.getError();
      }
      if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException
          || t instanceof TimeoutException) {
        return new FetchError(Kind.TIMEOUT, null);
      }
    }
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof IOException) {
        return new FetchError(Kind.CONNECTION_ERROR, null);
      }
    }
    return new FetchError(Kind.INVALID_RESPONSE, null);
  }

  /**
   * Gives the error as a string, for example {@code http_status 503} or {@code timeout}.
   *
   * @return the error as a string
   */
  @Override
  public String toString() {
    final String name = this.kind.name().toLowerCase();
    return this.httpStatus != null ? name + " " + this.httpStatus : name;
  }
}
