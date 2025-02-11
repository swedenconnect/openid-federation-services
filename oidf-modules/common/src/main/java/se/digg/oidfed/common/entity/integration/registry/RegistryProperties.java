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

import lombok.Builder;
import se.digg.oidfed.common.entity.integration.registry.records.EntityRecord;
import se.digg.oidfed.common.entity.integration.registry.records.PolicyRecord;

import java.util.List;
import java.util.UUID;

/**
 * Properties for registry integration.
 *
 * @param instanceId
 * @param trustMarkIssuerProperties
 * @param trustAnchorProperties
 * @param resolverProperties
 * @param entityRecords
 * @param policyRecords
 *
 * @author Felix Hellman
 */
@Builder
public record RegistryProperties(
    UUID instanceId,
    List<TrustMarkIssuerProperties> trustMarkIssuerProperties,
    List<TrustAnchorProperties> trustAnchorProperties,
    List<ResolverProperties> resolverProperties,
    List<EntityRecord> entityRecords,
    List<PolicyRecord> policyRecords
) {
}
