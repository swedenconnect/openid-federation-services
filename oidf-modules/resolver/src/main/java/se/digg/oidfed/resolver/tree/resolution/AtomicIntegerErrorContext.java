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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link ErrorContext} using {@link AtomicInteger} to keep track of state.
 *
 * @author Felix Hellman
 */
public class AtomicIntegerErrorContext implements ErrorContext {

  private final String location;
  private final AtomicInteger count = new AtomicInteger(0);

  /**
   * Constructor.
   * @param location of the error
   */
  public AtomicIntegerErrorContext(final String location) {
    this.location = location;
  }

  @Override
  public ErrorContext increment() {
    count.incrementAndGet();
    return this;
  }

  @Override
  public int getErrorCount() {
    return count.get();
  }

  @Override
  public String getLocation() {
    return location;
  }
}
