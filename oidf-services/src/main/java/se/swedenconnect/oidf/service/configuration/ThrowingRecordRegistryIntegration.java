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
package se.swedenconnect.oidf.service.configuration;

import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.ModuleRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RecordRegistryIntegration;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkRecord;

import java.util.List;
import java.util.UUID;

/**
 * Implementation used when integration is disabled.
 *
 * @author Felix Hellman
 */
public class ThrowingRecordRegistryIntegration implements RecordRegistryIntegration {
  @Override
  public Expirable<List<EntityRecord>> getEntityRecords(final UUID instanceID) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }

  @Override
  public Expirable<ModuleRecord> getModules(final UUID instanceId) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }

}
