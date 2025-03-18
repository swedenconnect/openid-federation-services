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
package se.swedenconnect.oidf.common.entity.entity.integration.properties;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkId;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectRecord;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;

import java.util.List;
import java.util.Optional;

/**
 * @param trustMarkId             The Trust Mark ID
 * @param logoUri                 Optional logo for issued Trust Marks
 * @param refUri                  Optional URL to information about issued Trust Marks
 * @param delegation              TrustMark delegation
 * @param trustMarkSubjectRecords subjects
 * @author Felix Hellman
 */
@Builder
public record TrustMarkProperties(TrustMarkId trustMarkId,
                                  Optional<String> logoUri,
                                  Optional<String> refUri,
                                  Optional<TrustMarkDelegation> delegation,
                                  List<TrustMarkSubjectRecord> trustMarkSubjectRecords) {
  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate () throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(this.trustMarkId, "TrustMarkId is expected");
    FederationAssert.assertNotEmpty(this.delegation, "Delegation can not be null");
    FederationAssert.assertNotEmpty(this.logoUri, "LogoUri can not be null");
    FederationAssert.assertNotEmpty(this.refUri, "RefUri can not be null");
  }
}
