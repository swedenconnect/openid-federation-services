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

import se.digg.oidfed.common.entity.integration.registry.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.registry.records.CompositeRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.ModuleRecord;
import se.digg.oidfed.common.entity.integration.registry.records.TrustMarkRecord;

import java.util.List;
import java.util.UUID;

public class CacheRecordPopulator {
  private final CachedRecordSource source;
  private final RecordRegistryIntegration integration;
  private Boolean notified;
  private final UUID instanceId;

  public CacheRecordPopulator(
      final CachedRecordSource source,
      final RecordRegistryIntegration integration,
      final UUID instanceId) {

    this.source = source;
    this.integration = integration;
    this.instanceId = instanceId;
  }

  public CompositeRecord reload() {
    //Clear notification
    this.notified = false;
    final Expirable<List<EntityRecord>> entityRecords = this.integration.getEntityRecords(this.instanceId);
    final Expirable<ModuleRecord> modules = this.integration.getModules(this.instanceId);
    final Expirable<List<TrustMarkRecord>> trustMarks = this.integration.getTrustMarks(this.instanceId);
    final CompositeRecord compositeRecord = new CompositeRecord(entityRecords, modules, trustMarks);
    this.source.addRecord(compositeRecord);
    return compositeRecord;
  }

  public boolean shouldRefresh() {
    return this.source.shouldRefresh() || this.notified;
  }

  public void notifyPopulator() {
    this.notified = true;
  }
}
