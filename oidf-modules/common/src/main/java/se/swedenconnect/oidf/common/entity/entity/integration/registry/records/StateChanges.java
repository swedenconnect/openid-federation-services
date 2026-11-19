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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import java.util.List;

/**
 * One page of changes from the change log of an instance in the registry.
 *
 * @param next    sequence number to continue paging after, or null on the last page
 * @param full    true if the pages from {@code since=0} hold every live key and replace the synced state
 * @param changes changes on this page, ordered by sequence number
 * @author Felix Hellman
 */
public record StateChanges(Long next, boolean full, List<Change> changes) {

  /**
   * Operation upserting a key.
   */
  public static final String UPSERT = "upsert";

  /**
   * Operation deleting a key.
   */
  public static final String DELETE = "delete";

  /**
   * A change of one key.
   *
   * @param seq    sequence number of the change
   * @param op     operation, {@link #UPSERT} or {@link #DELETE}
   * @param key    of the record
   * @param record the record json for {@link #UPSERT}, null for {@link #DELETE}
   */
  public record Change(long seq, String op, String key, String record) {
  }
}
