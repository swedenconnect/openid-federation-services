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

import com.nimbusds.jwt.SignedJWT;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Statement chain validation.
 * Validates a chain of entity statments by executing validation steps {@link ChainValidationStep} upon the chain.
 *
 * @author Felix Hellman
 */
public class ChainValidator {

  private final List<ChainValidationStep> chainValidationSteps;

  /**
   * Constructor
   *
   * @param chainValidationSteps to execute upon every chain
   */
  public ChainValidator(final List<ChainValidationStep> chainValidationSteps) {
    this.chainValidationSteps = chainValidationSteps;
  }

  /**
   * Validates a trust chain. Every validation step is executed, also when an earlier step has failed, so that all
   * problems of the chain are reported.
   *
   * @param chain to validate
   * @return the validation result, holding all errors found
   */
  public ChainValidationResult validate(final List<SignedJWT> chain) {
    final List<ChainValidationError> typedErrors = new ArrayList<>();
    if (chain.size() < 3) {
      typedErrors.add(new ChainValidationError(ChainValidationErrorType.CHAIN_TOO_SHORT,
          "Chain does not include at least three statements", null));
    }
    return this.runSteps(chain, typedErrors);
  }

  /**
   * Validates the Entity Configuration of the Trust Anchor on its own, used when the Trust Anchor itself is resolved.
   * The same validation steps as for a trust chain are run, so the signature is checked against the configured
   * trusted keys, together with {@code iat}, {@code exp} and {@code crit}.
   *
   * @param entityConfiguration the Entity Configuration of the Trust Anchor
   * @return the validation result
   */
  public ChainValidationResult validateTrustAnchor(final SignedJWT entityConfiguration) {
    return this.runSteps(List.of(entityConfiguration), new ArrayList<>());
  }

  private ChainValidationResult runSteps(final List<SignedJWT> chain, final List<ChainValidationError> typedErrors) {
    if (!chain.isEmpty()) {
      this.chainValidationSteps.forEach(step -> typedErrors.addAll(execute(step, chain)));
    }
    final List<Exception> errors = new ArrayList<>();
    if (!typedErrors.isEmpty()) {
      errors.add(new ChainValidationFailedException("Failed to validate trust chain: %s"
          .formatted(typedErrors.stream().map(Throwable::getMessage).toList())));
    }
    return new ChainValidationResult(chain, errors, List.copyOf(typedErrors));
  }

  /**
   * Executes a validation step. A step that fails unexpectedly is reported as an error of its own, so that the other
   * steps are still executed.
   *
   * @param step the step to execute
   * @param chain the chain to validate
   * @return the errors found by the step
   */
  private static List<ChainValidationError> execute(final ChainValidationStep step, final List<SignedJWT> chain) {
    try {
      return step.validate(chain);
    } catch (final Exception e) {
      return List.of(new ChainValidationError(ChainValidationErrorType.VALIDATION_STEP_FAILED,
          "%s failed: %s".formatted(step.getClass().getSimpleName(), e.getMessage()), e));
    }
  }
}
