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

import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.Cache;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;

import java.util.List;
import java.util.UUID;

/**
 * Refresh ahead cache for registry.
 *
 * @author Felix Hellman
 */
public class RegistryRefreshAheadCache {
  private final Cache<String, ModuleRecord> modules;
  private final Cache<String, List<TrustMarkSubjectRecord>> trustMarkSubjects;
  private final Cache<String, List<EntityRecord>> entityRecords;
  private final Cache<String, PolicyRecord> policyRecords;

  /**
   * @param modules
   * @param trustMarkSubjects
   * @param entityRecords
   * @param policyRecords
   */
  public RegistryRefreshAheadCache(
      final Cache<String, ModuleRecord> modules,
      final Cache<String,
          List<TrustMarkSubjectRecord>> trustMarkSubjects,
      final Cache<String, List<EntityRecord>> entityRecords,
      final Cache<String, PolicyRecord> policyRecords) {
    this.modules = modules;
    this.trustMarkSubjects = trustMarkSubjects;
    this.entityRecords = entityRecords;
    this.policyRecords = policyRecords;
  }

  /**
   * @param instanceId of this instance
   * @param response for modules
   */
  public void registerModule(final UUID instanceId, final Expirable<ModuleRecord> response) {
    this.modules.add(instanceId.toString(), response);
  }

  /**
   * @param instanceId of this instance
   * @return response for modules
   */
  public ModuleRecord getModules(final UUID instanceId) {
    return this.modules.get(instanceId.toString());
  }

  /**
   * @param instanceId to check
   * @return true if response empty or expired
   */
  public boolean modulesNeedsRefresh(final UUID instanceId) {
    return this.modules.shouldRefresh(instanceId.toString());
  }

  /**
   * @param key of trust mark subject
   * @param trustMarkSubject to add
   */
  public void registerTrustMarkSubjects(final TrustMarkSubjectKey key,
                                        final Expirable<List<TrustMarkSubjectRecord>> trustMarkSubject) {
    this.trustMarkSubjects.add(key.toString(), trustMarkSubject);
  }

  /**
   * @param issuer for entities
   * @param entityRecords to add
   */
  public void registerIssuerEntities(final String issuer, final Expirable<List<EntityRecord>> entityRecords) {
    this.entityRecords.add(issuer, entityRecords);
  }

  /**
   * @param issuer of entities
   * @return list of entities for a given issuer
   */
  public List<EntityRecord> getEntities(final String issuer) {
    return this.entityRecords.get(issuer);
  }

  /**
   * @param id of policy
   * @return record
   */
  public PolicyRecord getPolicy(final String id) {
    return this.policyRecords.get(id);
  }

  /**
   * @param id key to add
   * @param policy value to add
   */
  public void registerPolicy(final String id, final Expirable<PolicyRecord> policy) {
    this.policyRecords.add(id, policy);
  }

  /**
   * @param key for trust mark subjects
   * @return list of trust mark subjects
   */
  public List<TrustMarkSubjectRecord> getTrustMarkSubjects(final TrustMarkSubjectKey key) {
    return this.trustMarkSubjects.get(key.toString());
  }
}
