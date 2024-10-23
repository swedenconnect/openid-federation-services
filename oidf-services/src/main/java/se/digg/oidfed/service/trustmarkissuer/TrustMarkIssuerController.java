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
package se.digg.oidfed.service.trustmarkissuer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.digg.oidfed.service.ApplicationModule;
import se.digg.oidfed.trustmarkissuer.TrustMarkIssuer;
import se.digg.oidfed.trustmarkissuer.TrustMarkListingRequest;
import se.digg.oidfed.trustmarkissuer.TrustMarkRequest;
import se.digg.oidfed.trustmarkissuer.TrustMarkStatusRequest;

/**
 * Controller for trust mark issuer.
 *
 * @author Felix Hellman
 */
@RestController
@ConditionalOnProperty(value = TrustMarkIssuerConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustMarkIssuerController implements ApplicationModule {

  private final TrustMarkIssuer trustMarkIssuer;

  /**
   * Constructor.
   *
   * @param trustMarkIssuer
   */
  public TrustMarkIssuerController(final TrustMarkIssuer trustMarkIssuer) {
    this.trustMarkIssuer = trustMarkIssuer;
  }

  /**
   * Retrieves the trust mark for a given entity.
   *
   * @param name the name of the federation entity
   * @param trustMarkId the ID of the trust mark
   * @param subject the subject of the trust mark
   * @return the trust mark or error response
   */
  @GetMapping(value = "/{name}/trust_mark", produces = "application/trust-mark+jwt")
  public String trustMark(
      @PathVariable(name = "name") final String name,
      @RequestParam(name = "trust_mark_id") final String trustMarkId,
      @RequestParam(name = "sub") final String subject) {
    return trustMarkIssuer.trustMark(new TrustMarkRequest(name, trustMarkId, subject));
  }

  /**
   * Retrieves trust mark listing for a given trust mark ID, and subject.
   *
   * @param name the name of the trust mark issuer entity
   * @param trustMarkId the ID of the trust mark
   * @param subject the subject of the trust mark (optional)
   * @return the trust mark listing or an error response
   */
  @GetMapping(value = "/{name}/trust_mark_listing", produces = MediaType.APPLICATION_JSON_VALUE)
  public String trustMarkListing(
      @PathVariable(name = "name") final String name,
      @RequestParam(name = "trust_mark_id") final String trustMarkId,
      @RequestParam(name = "sub", required = false) final String subject) {
    return trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest(name, trustMarkId, subject));
  }

  /**
   * Retrieves the status of a trust mark.
   *
   * @param name the name of the trust mark issuer
   * @param trustMarkId the ID of the trust mark (optional)
   * @param subject the subject of the trust mark (optional)
   * @param issueTime the issue time of the trust mark (optional)
   * @param trustMark a trust mark (optional)
   * @return the trust mark status or an error response
   */
  @PostMapping(value = "/{name}/trust_mark_status", produces = MediaType.APPLICATION_JSON_VALUE)
  public String trustMarkStatus(
      @PathVariable(name = "name") final String name,
      @RequestParam(name = "trust_mark_id", required = false) final String trustMarkId,
      @RequestParam(name = "sub", required = false) final String subject,
      @RequestParam(name = "iat", required = false) final Long issueTime,
      @RequestParam(name = "trust_mark", required = false) final String trustMark) {
    return trustMarkIssuer.trustMarkStatus(
        new TrustMarkStatusRequest(name, trustMarkId, subject, issueTime, trustMark));
  }

  @Override
  public String getModuleName() {
    return "TrustMarkIssuer";
  }
}
