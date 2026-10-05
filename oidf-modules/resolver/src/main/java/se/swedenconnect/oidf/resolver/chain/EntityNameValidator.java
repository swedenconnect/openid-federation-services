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

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Matches entity identifiers against naming constraints. Constraints use the domain name syntax of RFC 5280,
 * Section 4.2.1.10, and apply to the host part of the entity identifier (OpenID Federation 1.0, Section 6.2.2).
 *
 * @author Felix Hellman
 */
@Slf4j
public class EntityNameValidator {

  /**
   * Checks an entity identifier against one naming constraint. A constraint starting with a period, such as
   * {@code .example.com}, matches any host below that domain but not the domain itself. A constraint without a
   * leading period, such as {@code host.example.com}, matches that host only.
   *
   * @param entityId the entity identifier to check
   * @param rule the naming constraint
   * @return true if the host of the entity identifier matches the constraint
   */
  public static boolean validate(final String entityId, final String rule) {
    if (rule == null || rule.isBlank()) {
      return false;
    }
    final String host = hostOf(entityId);
    if (host == null) {
      log.debug("Entity identifier '{}' has no host, it matches no naming constraint", entityId);
      return false;
    }
    final String constraint = rule.toLowerCase(Locale.ROOT);
    return constraint.startsWith(".") ? host.endsWith(constraint) : host.equals(constraint);
  }

  /**
   * Checks an entity identifier against a list of naming constraints.
   *
   * @param entityId the entity identifier to check
   * @param rules the naming constraints
   * @return true if any of the constraints matches
   */
  public static boolean anyMatch(final String entityId, final List<String> rules) {
    return rules.stream().anyMatch(rule -> validate(entityId, rule));
  }

  private static String hostOf(final String entityId) {
    try {
      return Optional.ofNullable(URI.create(entityId).getHost())
          .map(host -> host.toLowerCase(Locale.ROOT))
          .orElse(null);
    }
    catch (final IllegalArgumentException e) {
      return null;
    }
  }
}
