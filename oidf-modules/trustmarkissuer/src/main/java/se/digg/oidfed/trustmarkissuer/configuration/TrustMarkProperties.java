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

package se.digg.oidfed.trustmarkissuer.configuration;

import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import lombok.*;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertNotEmpty;
import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertTrue;

/**
 * Properties for TrustMarkIssuer
 *
 * @author Per Fredrik Plars
 */
@Builder
@ToString
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustMarkProperties {

  /** The validity duration of issued Trust Marks */
  private Duration trustMarkValidityDuration;

  /** */
  private String issuerEntityId;

  /**
   * Key used to sign this trustmark
   */
  private String signKey;

  /**
   * TrustMark Issuer
   */
  private List<TrustMarkIssuerProperties> trustMarks;

  /**
   * TrustMark sign key, must contain kid
   * @return JWK
   */
  public JWK getSignJWK() {
    try {
      return JWK.parse(new String(Base64.getDecoder().decode(signKey), Charset.defaultCharset()));
    }
    catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse signkey",e);
    }
  }

  /**
   * Validate content of configuration.
   * @throws IllegalArgumentException is thrown if something is not right in configuration
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    assertNotEmpty(trustMarkValidityDuration, "TrustMarkValidityDuration is expected");
    assertNotEmpty(issuerEntityId, "IssuerEntityId is expected");
    assertNotEmpty(trustMarks, "TrustMarks is expected");
    assertTrue(trustMarkValidityDuration.minus(Duration.ofMinutes(4)).isPositive(),
        "Expect trustMarkValidityDuration to be grater then 5 minutes");
    getSignJWK();
  }
}
