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

import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkDelegation;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.validation.FederationAssert;

import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static se.digg.oidfed.trustmarkissuer.validation.FederationAssert.assertTrue;

/**
 * Properties for TrustMarks.
 * https://openid.net/specs/openid-federation-1_0.html#section-7.1
 *
 * @author Per Fredrik Plars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Slf4j
public class TrustMarkIssuerProperties implements Serializable {
  /** The Trust Mark ID */
  private TrustMarkId trustMarkId;
  /** An optional logo for issued Trust Marks */
  private String logoUri;
  /** Optional URL to information about issued Trust Marks */
  private String refUri;
  /** List of SubjectProperties*/
  private List<TrustMarkIssuerSubjectProperties> subjects;
  /** TrustMark delegation */
  private TrustMarkDelegation delegation;
  /**
   * Validate content of configuration.
   * @throws IllegalArgumentException is thrown if something is not right in configuration
   */
  @PostConstruct
  public void validate() throws IllegalArgumentException {
    FederationAssert.assertNotEmpty(trustMarkId, "TrustMarkId is expected");
    FederationAssert.assertNotEmpty(subjects, "TrustMarkIssuerProperties is expected");
  }

  /**
   * TrustMarkIssuerSubjectProperties
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class TrustMarkIssuerSubjectProperties implements Serializable {
    private String sub;
    private Instant granted;
    private Instant expires;
    private boolean revoked;
    /**
     * Validate content of configuration.
     * @throws IllegalArgumentException is thrown if something is not right in configuration
     */
    @PostConstruct
    public void validate() {
      FederationAssert.assertNotEmpty(sub, "Subject is expected");
      if(granted != null && expires != null) {
        assertTrue(granted.isBefore(expires), "Expires expected to be after granted");
        if (expires.isAfter(Instant.now())) {
          log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'", sub, expires);
        }
      }
    }
  }
}
