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

import java.io.Serializable;
import java.util.List;

/**
 * Record class for trust mark.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
@Setter
public class TrustMarkProperty implements Serializable {
  private final String trustMarkIssuerId;
  private final String trustMarkId;
  private final List<TrustMarkSubjectProperty> subjects;
  private final String logoUri;
  private final String ref;
  private final String delegation;

  /**
   * Constructor.
   * @param trustMarkIssuerId
   * @param trustMarkId
   * @param subjects
   * @param logoUri
   * @param ref
   * @param delegation
   */
  public TrustMarkProperty(final String trustMarkIssuerId,
                           final String trustMarkId,
                           final List<TrustMarkSubjectProperty> subjects,
                           final String logoUri,
                           final String ref,
                           final String delegation) {
    this.trustMarkIssuerId = trustMarkIssuerId;
    this.trustMarkId = trustMarkId;
    this.subjects = subjects;
    this.logoUri = logoUri;
    this.ref = ref;
    this.delegation = delegation;
  }
}
