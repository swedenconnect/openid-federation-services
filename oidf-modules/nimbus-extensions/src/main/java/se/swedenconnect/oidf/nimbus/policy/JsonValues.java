/*
 * Copyright 2026 Sweden Connect
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
package se.swedenconnect.oidf.nimbus.policy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Comparison and copying of JSON values. Numbers are equal when they have the same numeric value.
 *
 * @author Felix Hellman
 */
final class JsonValues {

  private JsonValues() {
  }

  static boolean equal(final Object first, final Object second) {
    if (first instanceof final Number a && second instanceof final Number b) {
      return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())) == 0;
    }
    if (first instanceof final List<?> a && second instanceof final List<?> b) {
      if (a.size() != b.size()) {
        return false;
      }
      for (int i = 0; i < a.size(); i++) {
        if (!equal(a.get(i), b.get(i))) {
          return false;
        }
      }
      return true;
    }
    return Objects.equals(first, second);
  }

  static boolean contains(final Collection<?> values, final Object value) {
    return values.stream().anyMatch(item -> equal(item, value));
  }

  static boolean containsAll(final Collection<?> values, final Collection<?> required) {
    return required.stream().allMatch(item -> contains(values, item));
  }

  static List<?> asList(final Object value) {
    if (value == null) {
      return List.of();
    }
    return value instanceof final List<?> list ? list : List.of(value);
  }

  static Object copy(final Object value) {
    return value instanceof final List<?> list ? new ArrayList<Object>(list) : value;
  }
}
