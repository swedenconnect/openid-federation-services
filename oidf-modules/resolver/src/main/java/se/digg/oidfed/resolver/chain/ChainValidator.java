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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
   * @param chainValidationSteps to execute upon every chain
   */
  public ChainValidator(final List<ChainValidationStep> chainValidationSteps) {
    this.chainValidationSteps = chainValidationSteps;
  }

  /**
   * @param chain to validate
   * @return a validation result if the chain passed validation
   */
  public ChainValidationResult validate(final List<EntityStatement> chain) {
    // Check that chain has at least length = 3
    if (chain.size() < 3) {
      throw new IllegalArgumentException("Chain does not include at least three statements");
    }

    final List<ChainValidationStepResult> failedValidationSteps =
        this.chainValidationSteps
            .stream()
            .map(step -> execute(step, chain))
            .filter(stepResult -> !stepResult.valid())
            .toList();

    if (!failedValidationSteps.isEmpty()) {
      final Set<String> failedValidationStepNames =
          failedValidationSteps.stream().map(ChainValidationStepResult::name).collect(Collectors.toSet());
      final List<String> messages = failedValidationSteps.stream().map(s -> s.error().getMessage()).toList();
      final String exceptionMessage =
          "Failed to validate trust chain:%s messages:%s".formatted(failedValidationStepNames, messages);
      throw new IllegalStateException(exceptionMessage);
    }

    return new ChainValidationResult(chain);
  }

  private static ChainValidationStepResult execute(final ChainValidationStep step, final List<EntityStatement> chain) {
    final String name = step.getClass().getCanonicalName();
    try {
      step.validate(chain);
      return ChainValidationStepResult.valid(name);
    }
    catch (final Exception e) {
      return ChainValidationStepResult.invalid(name, e);
    }
  }
}
