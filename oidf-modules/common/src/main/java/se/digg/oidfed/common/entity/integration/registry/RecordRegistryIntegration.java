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
package se.digg.oidfed.common.entity.integration.registry;

import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.util.List;
import java.util.UUID;

/**
 * Interface for integrating towards a record registry.
 *
 * @author Felix Hellman
 */
public interface RecordRegistryIntegration {
  /**
   * @param id of the policy
   * @return policy if present in registry
   * @throws RegistryResponseException
   */
  Expirable<PolicyRecord> getPolicy(final String id);

  /**
   * @param issuer for the records
   * @return list of records
   * @throws RegistryResponseException
   */
  Expirable<List<EntityRecord>> getEntityRecords(final String issuer);

  /**
   * Fetches which modules should be configured for this instance.
   * @param instanceId of this instance
   * @return modules
   */
  Expirable<ModuleResponse> getModules(final UUID instanceId);

  /**
   * Fetches what subjects should be configured for a given trust-mark
   * @param issuer of the trust mark
   * @param trustMarkId id of the trust mark
   * @return list of subjects
   */
  Expirable<List<TrustMarkSubject>> getTrustMarkSubject(final String issuer, final String trustMarkId);
}
