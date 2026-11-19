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
package se.swedenconnect.oidf.resolver.chain;

import se.swedenconnect.oidf.common.entity.exception.InvalidTrustChainException;

/**
 * Summarizes the errors found when validating a trust chain. The individual errors are reported as
 * {@link ChainValidationError}.
 *
 * @author Felix Hellman
 */
public class ChainValidationFailedException extends InvalidTrustChainException {

  /**
   * Constructor.
   *
   * @param errorDescription human readable description
   */
  public ChainValidationFailedException(final String errorDescription) {
    super(errorDescription);
  }
}
