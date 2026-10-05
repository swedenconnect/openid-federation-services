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
package se.swedenconnect.oidf.resolver.metadata;

/**
 * Policy error raised while parsing, merging or applying a metadata policy (OpenID Federation 1.0, Section 6.1).
 * A policy error makes the trust chain invalid.
 *
 * @author Martin Lindström
 */
public class MetadataPolicyException extends Exception {

  /**
   * Constructor.
   *
   * @param message description of the error
   */
  public MetadataPolicyException(final String message) {
    super(message);
  }
}
