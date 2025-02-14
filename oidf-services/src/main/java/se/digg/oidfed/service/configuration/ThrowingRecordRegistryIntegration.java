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
package se.digg.oidfed.service.configuration;

import se.digg.oidfed.common.entity.integration.Expirable;
import se.digg.oidfed.common.entity.integration.registry.ModuleResponse;
import se.digg.oidfed.common.entity.integration.registry.RecordRegistryIntegration;
import se.digg.oidfed.common.entity.integration.registry.TrustMarkSubjectRecord;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;

import java.util.List;
import java.util.UUID;

/**
 * Implementation used when integration is disabled.
 *
 * @author Felix Hellman
 */
public class ThrowingRecordRegistryIntegration implements RecordRegistryIntegration {
  @Override
  public Expirable<PolicyRecord> getPolicy(final String id) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }

  @Override
  public Expirable<List<EntityRecord>> getEntityRecords(final String issuer) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }

  @Override
  public Expirable<ModuleResponse> getModules(final UUID instanceId) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }

  @Override
  public Expirable<List<TrustMarkSubjectRecord>> getTrustMarkSubject(final String issuer, final String trustMarkId) {
    throw new IllegalStateException("Client is configured to not contact registry, this method should not be called");
  }
}
