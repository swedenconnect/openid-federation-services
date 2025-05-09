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
package se.swedenconnect.oidf.resolver.tree.resolution;

import java.util.function.Supplier;

/**
 * Error context for keeping track of number of failures for a step.
 *
 * @author Felix Hellman
 */
public interface ErrorContext {
  /**
   * Increments internal counter and returns the state.
   * @return current instance
   */
  ErrorContext increment();

  /**
   * @return nr of errors
   */
  int getErrorCount();

  /**
   * @return true if nr of errors is equal to 0
   */
  default boolean isEmpty() {
    return this.getErrorCount() == 0;
  }

  /**
   * @param supplier of error context if missing
   * @return context
   */
  default ErrorContext orElseGet(Supplier<ErrorContext> supplier) {
    if (this.isEmpty()) {
      return supplier.get();
    }
    return this;
  }
}
