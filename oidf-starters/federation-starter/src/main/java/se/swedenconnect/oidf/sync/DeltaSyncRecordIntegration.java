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
package se.swedenconnect.oidf.sync;

import se.swedenconnect.oidf.RestClientRecordIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RecordRegistryIntegration} that keeps a synced copy of the records of each instance and only fetches what
 * changed since the last sync.
 *
 * @author Felix Hellman
 */
public class DeltaSyncRecordIntegration implements RecordRegistryIntegration {

  private final RestClientRecordIntegration client;
  private final RegistrySyncStateStore store;
  private final RegistryStateAssembler assembler;
  private final Map<UUID, RegistryStateSynchronizer> synchronizers = new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   * @param client     for fetching from the registry
   * @param store      for the synced state
   * @param assembler  for assembling records from the synced state
   */
  public DeltaSyncRecordIntegration(
      final RestClientRecordIntegration client,
      final RegistrySyncStateStore store,
      final RegistryStateAssembler assembler) {

    this.client = client;
    this.store = store;
    this.assembler = assembler;
  }

  @Override
  public Expirable<List<EntityRecord>> getEntityRecords(final UUID instanceId) {
    final Expirable<RegistrySyncState> state = this.synchronizer(instanceId).sync();
    return new Expirable<>(state.getExpiration(), state.getIssuedAt(),
        this.assembler.entityRecords(state.getValue()));
  }

  @Override
  public Expirable<ModuleRecord> getModules(final UUID instanceId) {
    final Expirable<RegistrySyncState> state = this.synchronizer(instanceId).sync();
    return new Expirable<>(state.getExpiration(), state.getIssuedAt(),
        this.assembler.moduleRecord(state.getValue()));
  }

  @Override
  public Optional<Boolean> hasChanges(final UUID instanceId) {
    return Optional.of(this.synchronizer(instanceId).hasChanges());
  }

  private RegistryStateSynchronizer synchronizer(final UUID instanceId) {
    return this.synchronizers.computeIfAbsent(instanceId,
        id -> new RegistryStateSynchronizer(id, this.client, this.store));
  }
}
