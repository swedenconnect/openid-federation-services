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
package se.swedenconnect.oidf.common.entity.entity.integration.registry;

import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkRecord;

import java.util.List;
import java.util.UUID;

/**
 * Interface for integrating towards a record registry.
 *
 * @author Felix Hellman
 */
public interface RecordRegistryIntegration {
  /**
   * @param instanceID that the record belongs to
   * @return list of records
   */
  Expirable<List<EntityRecord>> getEntityRecords(final UUID instanceID);

  /**
   * Fetches which modules should be configured for this instance.
   *
   * @param instanceId of this instance
   * @return modules
   */
  Expirable<ModuleRecord> getModules(final UUID instanceId);

  /**
   * Fetches what trust marks that should be configured
   *
   * @param instanceId of this instance
   * @return list of subjects
   */
  Expirable<List<TrustMarkRecord>> getTrustMarks(final UUID instanceId);
}
