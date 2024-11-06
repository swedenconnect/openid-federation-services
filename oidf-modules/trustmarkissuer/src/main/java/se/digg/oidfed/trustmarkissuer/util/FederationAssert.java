/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for assertions.
 *
 * @author Per Fredrik Plars
 */
public final class FederationAssert {

  public static <V> V assertNotEmpty(V value, String message) {
    return switch (value) {
      case null -> throw new IllegalArgumentException(message);
      case String s when s.isBlank() -> throw new IllegalArgumentException(message);
      case List list when list.isEmpty() -> throw new IllegalArgumentException(message);
      case Collection<?> objects when objects.isEmpty() -> throw new IllegalArgumentException(message);
      default -> value;
    };
  }

  public static <V, E extends Exception> V assertNotEmptyThrows(V value, final Supplier<E> ex) throws E {
    return switch (value) {
      case null -> throw ex.get();
      case String s when s.isBlank() -> throw ex.get();
      case List<?> list when list.isEmpty() -> throw ex.get();
      case Collection<?> objects when objects.isEmpty() -> throw ex.get();
      default -> value;
    };
  }

  public static void assertTrue(Boolean value, String message) {
    assertNotEmpty(value, message);
    if (!value) {
      throw new IllegalArgumentException(message);
    }
  }

  public static <V> void doIfNotNull(V value, Consumer<V> doOperation) {
    if (value != null) {
      doOperation.accept(value);
    }
  }

  public static <V, E extends Exception> void throwIfNull(V value, Consumer<V> doOperation, final Supplier<E> ex)
      throws E {
    assertNotEmptyThrows(value, ex);
    doOperation.accept(value);
  }

}
