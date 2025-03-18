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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of {@link StepRecoveryStrategy} that collects all errors and retries
 * when the {@link DeferredStepRecoveryStrategy#retry()} method is called.
 *
 * @author Felix Hellman
 */
public class DeferredStepRecoveryStrategy implements StepRecoveryStrategy {

  private final List<StepExecutionError> stepExecutionErrors = new CopyOnWriteArrayList<>();

  @Override
  public void handle(final StepExecutionError executionError) {
    this.stepExecutionErrors.add(executionError);
  }

  /**
   * Retries failed steps.
   */
  public void retry() {
    while (!this.stepExecutionErrors.isEmpty()) {
      final StepExecutionError step = this.stepExecutionErrors.removeFirst();
      step.getStep().accept(step.getErrorContext());
    }
  }
}
