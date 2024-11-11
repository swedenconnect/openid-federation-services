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
import se.digg.oidfed.trustmarkissuer.exception.InvalidRequestException;
import se.digg.oidfed.trustmarkissuer.exception.NotFoundException;
import se.digg.oidfed.trustmarkissuer.exception.ServerErrorException;

import java.util.List;

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
   * @param trustMarkId the ID of the trust mark
   * @param subject the subject of the trust mark
   * @return the trust mark or error response
   */
  @GetMapping(value = "/trust_mark", produces = "application/trust-mark+jwt")
  public String trustMark(
      @RequestParam(name = "trust_mark_id") final String trustMarkId,
      @RequestParam(name = "sub") final String subject)
      throws NotFoundException, InvalidRequestException, ServerErrorException {
    return trustMarkIssuer.trustMark(new TrustMarkRequest(trustMarkId, subject));
  }

  /**
   * Retrieves trust mark listing for a given trust mark ID, and subject.
   *
   * @param trustMarkId the ID of the trust mark
   * @param subject the subject of the trust mark (optional)
   * @return the trust mark listing or an error response
   */
  @GetMapping(value = "/trust_mark_listing", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> trustMarkListing(
      @RequestParam(name = "trust_mark_id") final String trustMarkId,
      @RequestParam(name = "sub", required = false) final String subject)
      throws NotFoundException, InvalidRequestException {
    return trustMarkIssuer.trustMarkListing(new TrustMarkListingRequest( trustMarkId, subject));
  }

  /**
   * Retrieves the status of a trust mark.
   *
   * @param trustMarkId the ID of the trust mark (optional)
   * @param subject the subject of the trust mark (optional)
   * @param issueTime the issue time of the trust mark (optional)
   * @return the trust mark status or an error response
   */
  @PostMapping(value = "/trust_mark_status", produces = MediaType.APPLICATION_JSON_VALUE)
  public TrustMarkStatusReply trustMarkStatus(
      @RequestParam(name = "trust_mark_id", required = true) final String trustMarkId,
      @RequestParam(name = "sub", required = false) final String subject,
      @RequestParam(name = "iat", required = false) final Long issueTime)
      throws NotFoundException, InvalidRequestException {

    return new TrustMarkStatusReply(trustMarkIssuer.trustMarkStatus(
        new TrustMarkStatusRequest( trustMarkId,subject, issueTime)));
  }

  /**
   * TrustMarkStatusReply indicates if trustmark is active or not
   * @param active true or false
   */
  public record TrustMarkStatusReply(Boolean active){}

  @Override
  public String getModuleName() {
    return "TrustMarkIssuer";
  }
}
