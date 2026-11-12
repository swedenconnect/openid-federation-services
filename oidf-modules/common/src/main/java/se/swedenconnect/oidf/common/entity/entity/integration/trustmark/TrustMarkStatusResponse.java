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

import java.time.Instant;

/**
 * The resolver's record of a trust mark status check. This is not the Trust Mark Status response defined by OpenID
 * Federation 1.0; that response is held as {@link #getSignedJWT()}. The other fields are internal state about the
 * check, and the record is never sent to anyone.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
public class TrustMarkStatusResponse {

  /** The status response from the trust mark issuer, or null if none was obtained. */
  private SignedJWT signedJWT;

  /** True if the status call failed. */
  private boolean error;

  /** True if the trust mark issuer publishes no status endpoint. */
  private boolean noStatusEndpoint;

  /** Time of the last status call that returned a status response, or null if none has. */
  private Instant lastChecked;

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
   * Creates a record for a trust mark issuer that publishes no status endpoint.
   *
   * @return record marking that no status endpoint exists
   */
  public static TrustMarkStatusResponse noStatusEndpoint() {
    final TrustMarkStatusResponse response = new TrustMarkStatusResponse(null, false);
    response.noStatusEndpoint = true;
    return response;
  }

  /**
   * Gets the status response from the trust mark issuer.
   *
   * @return the status response, or null if none was obtained
   */
  public SignedJWT getSignedJWT() {
    return this.signedJWT;
  }

  /**
   * Sets the status response from the trust mark issuer.
   *
   * @param signedJWT the status response, or null
   */
  public void setSignedJWT(final SignedJWT signedJWT) {
    this.signedJWT = signedJWT;
  }

  /**
   * Tells whether the status call failed.
   *
   * @return true if the status call failed
   */
  public boolean isError() {
    return this.error;
  }

  /**
   * Sets whether the status call failed.
   *
   * @param error true if the status call failed
   */
  public void setError(final boolean error) {
    this.error = error;
  }

  /**
   * Tells whether the trust mark issuer publishes no status endpoint.
   *
   * @return true if there is no status endpoint
   */
  public boolean isNoStatusEndpoint() {
    return this.noStatusEndpoint;
  }

  /**
   * Sets whether the trust mark issuer publishes no status endpoint.
   *
   * @param noStatusEndpoint true if there is no status endpoint
   */
  public void setNoStatusEndpoint(final boolean noStatusEndpoint) {
    this.noStatusEndpoint = noStatusEndpoint;
  }

  /**
   * Gets the time of the last status call that returned a status response.
   *
   * @return the time, or null if no call has returned a status response
   */
  public Instant getLastChecked() {
    return this.lastChecked;
  }

  /**
   * Sets the time of the last status call that returned a status response.
   *
   * @param lastChecked the time, or null
   */
  public void setLastChecked(final Instant lastChecked) {
    this.lastChecked = lastChecked;
  }
}
