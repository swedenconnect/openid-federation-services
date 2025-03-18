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
package se.swedenconnect.oidf.common.entity.entity.integration;

import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.CompositeRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkRecord;

import java.util.List;
import java.util.UUID;

/**
 * Responsible for populating cache from registry.
 *
 * @author Felix Hellman
 */
public class CacheRecordPopulator {
  private final CachedRecordSource source;
  private final RecordRegistryIntegration integration;
  private Boolean notified = false;
  private final UUID instanceId;

  /**
   * Constructor.
   * @param source to populate
   * @param integration to get information from
   * @param instanceId key for fetching information
   */
  public CacheRecordPopulator(
      final CachedRecordSource source,
      final RecordRegistryIntegration integration,
      final UUID instanceId) {

    this.source = source;
    this.integration = integration;
    this.instanceId = instanceId;
  }

  /**
   * @return state
   */
  public CompositeRecord reload() {
    final Expirable<List<EntityRecord>> entityRecords = this.integration.getEntityRecords(this.instanceId);
    final Expirable<ModuleRecord> modules = this.integration.getModules(this.instanceId);
    final Expirable<List<TrustMarkRecord>> trustMarks = this.integration.getTrustMarks(this.instanceId);
    final CompositeRecord compositeRecord = new CompositeRecord(entityRecords, modules, trustMarks);
    this.source.addRecord(compositeRecord);
    //Clear notification
    this.notified = false;
    return compositeRecord;
  }

  /**
   * @return To check if
   */
  public boolean shouldRefresh() {
    return this.source.shouldRefresh() || this.notified;
  }

  /**
   * Sets notification to true.
   */
  public void notifyPopulator() {
    this.notified = true;
  }
}
