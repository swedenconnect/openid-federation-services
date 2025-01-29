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
package se.digg.oidfed.common.entity.integration;

import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.PolicyRecord;

import java.util.List;
import java.util.Optional;

/**
 * Interface for record registry integration/cache
 *
 * @author Felix Hellman
 */
public interface RecordRegistrySource {

  /**
   * @param id of the policy
   * @return policy if such a policy exits
   */
  Optional<PolicyRecord> getPolicy(final String id);

  /**
   * @param issuer for the entity records
   * @return records
   */
  List<EntityRecord> getEntityRecords(final String issuer);


  /**
   * Locally adds a policy record
   * @param record to add
   */
  void addPolicy(final PolicyRecord record);
}
