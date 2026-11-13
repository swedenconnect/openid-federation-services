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
package se.swedenconnect.oidf.sync;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Synced records of an instance, by key, and the sequence number they were synced up to.
 *
 * @param seq     sequence number of the last applied change
 * @param records record json by key, ordered by key
 * @author Felix Hellman
 */
public record RegistrySyncState(long seq, SortedMap<String, String> records) {

  /**
   * Constructor, keeps an unmodifiable copy of the records.
   *
   * @param seq     sequence number of the last applied change
   * @param records record json by key
   */
  public RegistrySyncState {
    records = Collections.unmodifiableSortedMap(new TreeMap<>(records));
  }
}
