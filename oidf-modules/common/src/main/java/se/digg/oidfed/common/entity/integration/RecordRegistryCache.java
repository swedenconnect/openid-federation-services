/*
 * Copyright 2024 Sweden Connect
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
package se.digg.oidfed.common.entity.integration;

import jakarta.annotation.Nullable;
import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.PolicyRecord;

import java.util.List;

/**
 * Interface for cache for record registry.
 *
 * @author Felix Hellman
 */
public interface RecordRegistryCache {
  /**
   * @param id of the policy
   * @return response about a policy
   */
  CacheResponse<PolicyRecord> getPolicy(final String id);

  /**
   * @param record to register
   * @param id to use as key
   */
  void registerPolicy(@Nullable final PolicyRecord record, final String id);

  /**
   * @param issuer for the records
   * @return cache response
   */
  CacheResponse<List<EntityRecord>> getEntityRecords(final String issuer);

  /**
   * @param records to add
   * @param issuer to add for a given key
   */
  void registerRecords(@Nullable final List<EntityRecord> records, final String issuer);
}
