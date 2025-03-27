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
package se.swedenconnect.oidf.resolver.chain;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Validation class for checking allowed/excluded entity name patterns.
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityNameValidator {
  /**
   * @param entityId to check
   * @param rule rule to check towards
   * @return true if allowed, false if not allowed
   */
  public static boolean validate(final String entityId, final String rule) {
    final URI permittedUri = URI.create(rule);
    final URI entityUri = URI.create(entityId);
    try {
      Optional.ofNullable(permittedUri.getAuthority()).ifPresent(authority -> {
        if (!entityUri.getAuthority().endsWith(authority)) {
          throw new IllegalArgumentException("Failed to validate Entity Name, illegal authority");
        }
      });
      Optional.ofNullable(permittedUri.getScheme()).ifPresent(scheme -> {
        if (!entityUri.getScheme().contains(scheme)) {
          throw new IllegalArgumentException("Failed to validate Entity Name, illegal scheme");
        }
      });
      Optional.ofNullable(permittedUri.getPath()).ifPresent(path -> {
        if (!entityUri.getPath().contains(path)) {
          throw new IllegalArgumentException("Failed to validate Entity Name, illegal path");
        }
      });
      //Everything matches
      return true;
    } catch (final IllegalArgumentException e) {
      log.warn("Entity name did not match constraint", e);
      return false;
    }
  }

  /**
   * Checks entityId towards a list of rules
   * @param entityId to check
   * @param rules to check
   * @return true if
   */
  public static boolean anyMatch(final String entityId, final List<String> rules) {
    final long failedRules = rules.stream().map(rule -> validate(entityId, rule))
        .filter(v -> !v)
        .count();

    return failedRules != rules.size();
  }
}
