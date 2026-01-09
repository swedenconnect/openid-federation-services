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

import lombok.Builder;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.ResolverProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustAnchorProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.properties.TrustMarkIssuerProperties;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.PolicyRecord;

import java.util.List;

/**
 * Properties for registry integration.
 *
 * @param trustMarkIssuerProperties for locally configured trust marks
 * @param trustAnchorProperties for locally configured trust anchors
 * @param resolverProperties for locally configures resolvers
 * @param entityRecords for locally configured entity records
 *
 * @author Felix Hellman
 */
@Builder
public record LocalRegistryProperties(
    List<TrustMarkIssuerProperties> trustMarkIssuerProperties,
    List<TrustAnchorProperties> trustAnchorProperties,
    List<ResolverProperties> resolverProperties,
    List<EntityRecord> entityRecords
) {
}
