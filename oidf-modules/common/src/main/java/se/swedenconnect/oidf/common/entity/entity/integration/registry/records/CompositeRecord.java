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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Record containing all records from the registry.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
public class CompositeRecord {
  private final Expirable<List<EntityRecord>> entityRecords;
  private final Expirable<ModuleRecord> moduleRecord;

  /**
   * @param entityRecords    with entities
   * @param moduleRecord     with modules
   */
  public CompositeRecord(
      final Expirable<List<EntityRecord>> entityRecords,
      final Expirable<ModuleRecord> moduleRecord) {

    this.entityRecords = entityRecords;
    this.moduleRecord = moduleRecord;
  }

  /**
   * @return expiration time of the closest expiration
   */
  public Instant getExpiration() {
    final ArrayList<Instant> expirations = new ArrayList<>();
    expirations.add(this.moduleRecord.getExpiration());
    expirations.add(this.entityRecords.getExpiration());
    return Optional.ofNullable(expirations.getFirst()).orElse(Instant.now());
  }

  /**
   * @return time when this record was issued at
   */
  public Instant getIssuedAt() {
    final ArrayList<Instant> issuedAt = new ArrayList<>();
    issuedAt.add(this.moduleRecord.getIssuedAt());
    issuedAt.add(this.entityRecords.getIssuedAt());
    return Optional.ofNullable(issuedAt.getFirst()).orElse(Instant.now());
  }
}
