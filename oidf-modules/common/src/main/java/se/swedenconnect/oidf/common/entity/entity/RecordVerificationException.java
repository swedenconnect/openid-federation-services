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
package se.swedenconnect.oidf.common.entity.entity;

/**
 * Exception for record verification failures.
 *
 * @author Felix Hellman
 */
public class RecordVerificationException extends RuntimeException {
  /**
   * Constructor.
   * @param message the detail message
   * @param cause the cause
   */
  public RecordVerificationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor.
   * @param message the detailed message
   */
  public RecordVerificationException(final String message) {
    super(message);
  }
}
