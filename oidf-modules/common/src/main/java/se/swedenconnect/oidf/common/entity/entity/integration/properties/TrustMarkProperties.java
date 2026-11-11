/*
 * Copyright 2024-2026 Sweden Connect
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

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkDelegation;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.TrustMarkSubjectProperty;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;

import java.time.Duration;
import java.util.List;

/**
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class TrustMarkProperties {

  /**
   * The shortest validity duration that is not likely to be a configuration mistake.
   */
  static final Duration MINIMUM_VALIDITY_DURATION = Duration.ofMinutes(5);

  @SerializedName("trust-mark-type")
  private TrustMarkType trustMarkType;
  @SerializedName("logo-uri")
  private String logoUri;
  @SerializedName("ref-uri")
  private String refUri;
  @SerializedName("delegation")
  private TrustMarkDelegation delegation;
  @SerializedName("trust-mark-subjects")
  private List<TrustMarkSubjectProperty> trustMarkSubjects;
  @SerializedName("trust-mark-validity-duration")
  private Duration trustMarkValidityDuration;

  /**
   * @return configured trust mark subjects, or an empty list if none are configured
   */
  public List<TrustMarkSubjectProperty> getTrustMarkSubjects() {
    return this.trustMarkSubjects == null ? List.of() : this.trustMarkSubjects;
  }

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(this.trustMarkType, "TrustMarkId is expected");
    assertValidityDuration(this.trustMarkValidityDuration);
  }

  /**
   * Asserts that a validity duration, when given, is long enough to be useful. A missing duration means that the
   * issued trust marks never expire.
   *
   * @param validityDuration the duration to check, may be null
   * @throws IllegalArgumentException if the duration is given and shorter than the minimum
   */
  static void assertValidityDuration(final Duration validityDuration) throws IllegalArgumentException {
    FederationAssert.assertTrue(!isShorterThanMinimum(validityDuration),
        "Expect trust mark validity duration to be at least %s. Current value:'%s'"
            .formatted(MINIMUM_VALIDITY_DURATION, validityDuration));
  }

  /**
   * Checks whether a validity duration is shorter than the minimum. A duration that is not given is not short, it
   * means that the issued trust marks never expire.
   *
   * @param validityDuration the duration to check, may be null
   * @return true if the duration is given and shorter than the minimum
   */
  static boolean isShorterThanMinimum(final Duration validityDuration) {
    return validityDuration != null && validityDuration.compareTo(MINIMUM_VALIDITY_DURATION) < 0;
  }
}
