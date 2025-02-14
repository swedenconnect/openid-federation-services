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

/**
 * Verifier class for verifying payloads from registry.
 *
 * @author Felix Hellman
 */
public interface RegistryVerifier {
  /**
   * @param jwt with entity records
   * @return list of entity records
   */
  Expirable<List<EntityRecord>> verifyEntityRecords(final String jwt);

  /**
   * @param jwt with policy record
   * @return policy record
   */
  Expirable<PolicyRecord> verifyPolicyRecord(final String jwt);

  /**
   * @param jwt with modules
   * @return modules
   */
  Expirable<ModuleResponse> verifyModuleResponse(final String jwt);

  /**
   * @param jwt with trust mark subjects
   * @return list of trust mark subjects
   */
  Expirable<List<TrustMarkSubjectRecord>> verifyTrustMarkSubjects(final String jwt);
}
