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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.TrustMarkType;
import se.swedenconnect.oidf.common.entity.validation.FederationAssert;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Properties for TrustMarkIssuer
 *
 * @param trustMarkValidityDuration The validity duration of issued Trust Marks, deprecated in favour of the
 *     validity duration of each trust mark. It is only used as a fallback for trust marks without a duration of
 *     their own, and is removed once the registry no longer sends it.
 * @param entityIdentifier            IssuerEntityId
 * @param trustMarks                TrustMark Issuer
 * @author Per Fredrik Plars
 */
@Builder
@Slf4j
public record TrustMarkIssuerProperties(
    @SerializedName("trust-mark-validity-duration") Duration trustMarkValidityDuration,
    @SerializedName("entity-identifier") EntityID entityIdentifier,
    @SerializedName("trust-marks") List<TrustMarkProperties> trustMarks) {

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(this.entityIdentifier, "IssuerEntityId is expected");
    FederationAssert.assertNotEmpty(this.trustMarks, "TrustMarks is expected");
    TrustMarkProperties.assertValidityDuration(this.trustMarkValidityDuration);

    this.trustMarks.forEach(TrustMarkProperties::validate);
  }

  /**
   * Logs a warning for each configured validity duration that is shorter than the minimum. Such a duration is still
   * used, it is accepted but is likely to be a configuration mistake. Called when records are loaded.
   */
  public void warnForShortValidityDurations() {
    if (TrustMarkProperties.isShorterThanMinimum(this.trustMarkValidityDuration)) {
      log.warn("Trust mark issuer {} has a trust mark validity duration of {}, shorter than the minimum of {}",
          this.entityIdentifier, this.trustMarkValidityDuration, TrustMarkProperties.MINIMUM_VALIDITY_DURATION);
    }
    Optional.ofNullable(this.trustMarks).orElseGet(List::of).stream()
        .filter(trustMark -> TrustMarkProperties.isShorterThanMinimum(trustMark.getTrustMarkValidityDuration()))
        .forEach(trustMark -> log.warn(
            "Trust mark {} of {} has a validity duration of {}, shorter than the minimum of {}",
            Optional.ofNullable(trustMark.getTrustMarkType()).map(TrustMarkType::getTrustMarkType).orElse("unknown"),
            this.entityIdentifier, trustMark.getTrustMarkValidityDuration(),
            TrustMarkProperties.MINIMUM_VALIDITY_DURATION));
  }
}
