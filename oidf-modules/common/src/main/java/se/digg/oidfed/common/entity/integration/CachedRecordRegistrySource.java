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

import se.digg.oidfed.common.entity.EntityRecord;
import se.digg.oidfed.common.entity.PolicyRecord;

import java.util.List;
import java.util.Optional;

/**
 * Loading cache for record registry.
 *
 * @author Felix Hellman
 */
public class CachedRecordRegistrySource implements RecordRegistrySource {

  private final RecordRegistryIntegration integration;
  private final RecordRegistryCache cache;

  /**
   * @param integration towards registry
   * @param cache for registry
   */
  public CachedRecordRegistrySource(final RecordRegistryIntegration integration, final RecordRegistryCache cache) {
    this.integration = integration;
    this.cache = cache;
  }

  @Override
  public Optional<PolicyRecord> getPolicy(final String id) {
    final CacheResponse<PolicyRecord> policy = this.cache.getPolicy(id);
    if (policy.data().isPresent()) {
      return policy.data();
    }
    if (policy.fetchOnMiss()) {
      final Optional<PolicyRecord> policyRecord = this.integration.getPolicy(id);
      this.cache.registerPolicy(policyRecord.orElse(null), id);
    }
    return Optional.empty();
  }

  @Override
  public List<EntityRecord> getEntityRecords(final String issuer) {
    final CacheResponse<List<EntityRecord>> entities = this.cache.getEntityRecords(issuer);
    if (entities.data().isPresent()) {
      return entities.data().get();
    }
    if (entities.fetchOnMiss()) {
      final List<EntityRecord> entityRecords = this.integration.getEntityRecords(issuer);
      this.cache.registerRecords(entityRecords, issuer);
      return entityRecords;
    }
    return List.of();
  }

  @Override
  public void addPolicy(final PolicyRecord record) {
    this.cache.addPolicy(record);
  }
}
