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
package se.swedenconnect.oidf.common.entity.entity.integration.trustmark;

import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import lombok.Setter;

/**
 * Response for a trust mark status check.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class TrustMarkStatusResponse {
  private SignedJWT signedJWT;
  private boolean error;
  private boolean noStatusEndpoint;

  /**
   * Constructor.
   *
   * @param signedJWT the status response, null if the status call failed
   * @param error true if the status call failed
   */
  public TrustMarkStatusResponse(final SignedJWT signedJWT, final boolean error) {
    this.signedJWT = signedJWT;
    this.error = error;
  }

  /**
   * Creates a response for a trust mark issuer that publishes no status endpoint.
   *
   * @return response marking that no status endpoint exists
   */
  public static TrustMarkStatusResponse noStatusEndpoint() {
    final TrustMarkStatusResponse response = new TrustMarkStatusResponse(null, false);
    response.noStatusEndpoint = true;
    return response;
  }
}
