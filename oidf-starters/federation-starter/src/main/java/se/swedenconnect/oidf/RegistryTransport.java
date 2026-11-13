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
 * Transport used for loading records from the registry.
 *
 * @author Felix Hellman
 */
public enum RegistryTransport {
  /**
   * Fetches all entity records and module records on every load.
   */
  LEGACY,
  /**
   * Keeps a synced copy of the records and only fetches what changed since the last load.
   */
  DELTA
}
