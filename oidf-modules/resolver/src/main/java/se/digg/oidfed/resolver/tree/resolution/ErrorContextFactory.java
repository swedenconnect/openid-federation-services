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


/**
 * Factory for creating error context.
 * Allows externally created error contexts for ease of monitoring.
 *
 * @author Felix Hellman
 */
@FunctionalInterface
public interface ErrorContextFactory {
  /**
   * Create error context with a given name.
   * @param errorName of the error
   * @return new instance of context
   */
  ErrorContext create(final String errorName);
}
