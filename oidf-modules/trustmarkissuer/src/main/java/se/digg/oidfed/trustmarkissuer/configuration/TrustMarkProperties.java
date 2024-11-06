/*
 *  Copyright 2024 Sweden Connect
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package se.digg.oidfed.trustmarkissuer.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import se.digg.oidfed.trustmarkissuer.util.FederationAssert;

import java.time.Duration;

import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertNotEmpty;
import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertTrue;

/**
 * openid-federation-services
 *
 * @author Per Fredrik Plars
 */
@EqualsAndHashCode
@Builder
@Getter
@ToString
public class TrustMarkProperties {

  /** The validity duration of issued Trust Marks */
  private Duration trustMarkValidityDuration;
  /** An optional logo for issued Trust Marks */
  private String logoUri;
  /** Optional URL to information about issued Trust Marks */
  private String refUrl;

  @PostConstruct
  public void validate() throws IllegalArgumentException {
    assertNotEmpty(trustMarkValidityDuration, "TrustMarkValidityDuration is expected");
    assertTrue(trustMarkValidityDuration.minus(Duration.ofMinutes(4)).isPositive(),"Expect trustMarkValidityDuration to be grater then 5 minutes");
    assertNotEmpty(logoUri, "LogoUri is expected");
    assertNotEmpty(refUrl, "RefUrl is expected");
  }
}
