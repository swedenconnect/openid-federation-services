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
package se.swedenconnect.oidf;

/**
 * How the resolver may build a trust chain at request time, instead of reading it from its tree.
 *
 * @author Felix Hellman
 */
public enum LazyResolve {
  /**
   * Trust chains are only read from the tree.
   */
  NONE,
  /**
   * A trust chain is built at request time when every statement in it, and the status of the leaf's trust marks,
   * come from entities this service hosts. Otherwise it is read from the tree.
   */
  LOCAL,
  /**
   * As {@link #LOCAL}, but the leaf's Entity Configuration and the status of its trust marks may also be fetched over
   * HTTP. The superiors must still be hosted by this service.
   */
  ALL
}
