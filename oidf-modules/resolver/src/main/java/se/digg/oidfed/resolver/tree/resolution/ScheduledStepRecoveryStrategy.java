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
package se.digg.oidfed.resolver.tree.resolution;

import se.digg.oidfed.resolver.ResolverProperties;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link StepRecoveryStrategy} to handle errors
 *
 * @author Felix Hellman
 */
public class ScheduledStepRecoveryStrategy implements StepRecoveryStrategy {
  private final ScheduledExecutorService executorService;
  private final ResolverProperties properties;

  /**
   * @param executorService to schedule retry
   * @param properties for configuration retry time
   */
  public ScheduledStepRecoveryStrategy(
      final ScheduledExecutorService executorService,
      final ResolverProperties properties
  ) {
    this.executorService = executorService;
    this.properties = properties;
  }

  @Override
  public void handle(final StepExecutionError executionError) {
    executorService.schedule(() -> executionError.getStep().accept(executionError.getErrorContext()),
        properties.stepRetryTime().getSeconds(),
        TimeUnit.SECONDS);
  }
}
