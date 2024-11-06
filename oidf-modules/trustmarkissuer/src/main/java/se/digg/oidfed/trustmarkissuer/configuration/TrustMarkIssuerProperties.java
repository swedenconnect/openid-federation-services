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

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.digg.oidfed.trustmarkissuer.dvo.TrustMarkId;
import se.digg.oidfed.trustmarkissuer.util.FederationAssert;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import static se.digg.oidfed.trustmarkissuer.util.FederationAssert.assertTrue;

/**
 * Properties for TrustMarks
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder(toBuilder = true)
@Slf4j
public class TrustMarkIssuerProperties implements Serializable {
  /** The Trust Mark ID */
  private TrustMarkId trustmarkid;

  /** List of SubjectProperties*/
  private List<TrustMarkIssuerSubjectProperties> subjects;

  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(trustmarkid, "TrustMarkId is expected");
    FederationAssert.assertNotEmpty(subjects, "TrustMarkIssuerProperties is expected");
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class TrustMarkIssuerSubjectProperties implements Serializable {
    private String sub;
    private Instant granted;
    private Instant expires;
    private boolean revoked;

    @PostConstruct
    public void validate() {
      FederationAssert.assertNotEmpty(sub, "Subject is expected");
      FederationAssert.assertNotEmpty(granted, "Granted is expected");
      FederationAssert.assertNotEmpty(expires, "Expires is expected");
      assertTrue(granted.isBefore(expires),"Expires expected to be after granted");
      if(expires.isAfter(Instant.now())){
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'",sub,expires);
      }
    }
  }
}
