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
 *
 */
package se.digg.oidfed.resolver.chain;

/**
 * Result of a given chain validation step.
 * @param name of the step
 * @param valid true if the step validation has completed with error
 * @param error the error that must be supplied if valid == false to indicate the source of the problem.
 *
 * @author Felix Hellman
 */
public record ChainValidationStepResult(String name, boolean valid, Throwable error) {

  /**
   * Factory method for creating a valid response.
   * @param name of the step
   * @return result instance
   */
  public static ChainValidationStepResult valid(final String name) {
    return new ChainValidationStepResult(name, true, null);
  }

  /**
   * Factory method for creating an invalid response.
   * @param name of the step
   * @param error of what went wrong
   * @return result instance
   */
  public static ChainValidationStepResult invalid(final String name, final Exception error) {
    return new ChainValidationStepResult(name, false, error);
  }
}
