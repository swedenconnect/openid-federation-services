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
package se.digg.oidfed.service.resolver.observability;

import io.micrometer.core.instrument.Counter;
import se.digg.oidfed.resolver.tree.resolution.ErrorContext;

/**
 * Wrapper for {@link ErrorContext} to add an external counter to expose the metric state.
 *
 * @author Felix Hellman
 */
public class ObservableErrorContext implements ErrorContext {
  private final ErrorContext context;
  private final Counter counter;

  /**
   * Constructor.
   * @param context to wrap
   * @param counter to use for metric
   */
  public ObservableErrorContext(final ErrorContext context, final Counter counter) {
    this.context = context;
    this.counter = counter;
  }

  @Override
  public ErrorContext increment() {
    this.counter.increment();
    return this.context.increment();
  }

  @Override
  public int getErrorCount() {
    return this.context.getErrorCount();
  }

  @Override
  public String getLocation() {
    return this.context.getLocation();
  }
}
