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
package se.digg.oidfed.common.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data class for hosted record.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
public class HostedRecord {
  private final Map<String, Object> metadata;
  private final List<TrustMarkSource> trustMarkSources;
  private final List<String> authorityHints;

  /**
   * Constructor.
   *
   * @param metadata         for this record
   * @param trustMarkSources for this record
   * @param authorityHints   for this record
   */
  public HostedRecord(
      final Map<String, Object> metadata,
      final List<TrustMarkSource> trustMarkSources,
      final List<String> authorityHints) {

    this.metadata = metadata;
    this.trustMarkSources = trustMarkSources;
    this.authorityHints = authorityHints;
  }

  /**
   * @return this instance converted to json (map) structure for nimbus
   */
  public Map<String, Object> toJson() {
    return Map.of(
        "authority_hints", Optional.ofNullable(authorityHints).orElseGet(List::of),
        "metadata", metadata,
        "trust_mark_sources", Optional.ofNullable(trustMarkSources)
            .map(tms -> tms.stream()
                .map(TrustMarkSource::toJson)
                .toList()).orElseGet(List::of)

    );
  }

  /**
   * @param json to create instance from
   * @return new instance
   */
  public static HostedRecord fromJson(final Map<String, Object> json) {
    if (json == null) {
      return null;
    }
    final Map<String, Object> map = (Map<String, Object>) json.get("metadata");
    final List<Map<String, Object>> tms = (List<Map<String, Object>>) json.get("trust_mark_sources");
    final List<String> authorityHints = (List<String>) json.get("authority_hints");
    return new HostedRecord(
        Optional.ofNullable(map).orElseGet(Map::of),
        Optional.ofNullable(tms).map(trustmarksource -> trustmarksource
            .stream()
            .map(TrustMarkSource::fromJson)
            .toList()
        ).orElseGet(List::of),
        Optional.ofNullable(authorityHints).orElseGet(List::of)
    );
  }
}
