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
package se.digg.oidfed.resolver.tree.resolution;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Exception class for StepExecutionErrors
 *
 * @author Felix Hellman
 */
@Getter
public class StepExecutionError extends RuntimeException {
  private final Consumer<ErrorContext> step;
  private final ErrorContext errorContext;

  /**
   * Constructor.
   *
   * @param message      of why the step failed
   * @param step         that was run so that it can be re-run if needed.
   * @param errorContext context for the failure
   */
  public StepExecutionError(final String message, final Consumer<ErrorContext> step, final ErrorContext errorContext) {
    super(message);
    this.step = step;
    this.errorContext = errorContext;
  }
}
