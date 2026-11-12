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
package se.swedenconnect.oidf.common.entity.tree.scraping;

/**
 * Receives the failed fetches of a scrape.
 *
 * @author Martin Lindström
 */
@FunctionalInterface
public interface ScrapeFailureListener {

  /**
   * The parts of an entity that are fetched.
   */
  enum Part {
    /** The Entity Configuration. */
    ENTITY_CONFIGURATION,
    /** The subordinate listing. */
    SUBORDINATE_LISTING,
    /** A Subordinate Statement. */
    SUBORDINATE_STATEMENT,
    /** The status of a trust mark. */
    TRUST_MARK_STATUS
  }

  /**
   * Called when fetching a part fails.
   *
   * @param part the part that failed
   * @param entityId the entity the part is about, for a Subordinate Statement its subject
   * @param reason description of the failure
   * @param previousUsed true if the part from the previous snapshot is used instead
   */
  void failed(Part part, String entityId, String reason, boolean previousUsed);
}
