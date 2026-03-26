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
package se.swedenconnect.oidf.common.entity.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record ScrapedTrustMarkIssuer(Map<String, ScrapedTrustMark> trustMark) {
  public Optional<ScrapedTrustMarkInfo> trustMarkInfo(final String trustMarkType, final String trustMarkSubject) {
    return Optional.ofNullable(this.trustMark().get(trustMarkType))
        .flatMap(tm -> Optional.ofNullable(tm.subjects()))
        .flatMap(tms -> Optional.ofNullable(tms.get(trustMarkSubject)));
  }

  public void addTrustMarkInfo(final ScrapedTrustMarkInfo info) {
    this.trustMark().computeIfAbsent(info.trustMarkType(), key -> {
      final ScrapedTrustMark addedTrustMark = new ScrapedTrustMark(info.trustMarkType(), new HashMap<>());
      addedTrustMark.subjects().put(info.trustMarkSubject(), info);
      return addedTrustMark;
    });
  }
}

