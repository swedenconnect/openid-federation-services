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
package se.swedenconnect.oidf.common.entity.entity;

import com.nimbusds.jwt.JWTClaimsSet;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

/**
 * Entity Configuration Claims Customizer.
 *
 * @author Felix Hellman
 */
@FunctionalInterface
public interface EntityConfigurationClaimCustomizer {
  /**
   * @param record of the enity
   * @param builder to customize
   * @return builder
   */
  JWTClaimsSet.Builder customize(final EntityRecord record, final JWTClaimsSet.Builder builder);
}
