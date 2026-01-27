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
package se.swedenconnect.oidf.resolver;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.resolver.tree.EntityStatementTreeLoader;
import se.swedenconnect.oidf.resolver.tree.resolution.AtomicIntegerErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContext;
import se.swedenconnect.oidf.resolver.tree.resolution.ErrorContextFactory;

import java.util.List;

/**
 * Implementation of {@link ErrorContextFactory} that creates contexts that exposes a {@link Counter} for monitoring
 * failures.
 *
 * @author Felix Hellman
 */
public class ObservableErrorContextFactory implements ErrorContextFactory {

  private final MeterRegistry registry;

  /**
   * Constructor.
   * @param registry for managing counters
   */
  public ObservableErrorContextFactory(final MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public ErrorContext create(final NodeKey key, final EntityStatementTreeLoader.StepName stepName) {
    final List<Tag> tags = List.of(Tag.of("key", key.getKey()), Tag.of("step", stepName.name()));
    final Counter counter = this.registry.counter("resolver_tree_step_failure", tags);
    return new ObservableErrorContext(new AtomicIntegerErrorContext(),counter);
  }

  @Override
  public ErrorContext createEmpty() {
    //No need to associate counter with empty context
    return new AtomicIntegerErrorContext();
  }
}
