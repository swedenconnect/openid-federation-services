/*
 * Copyright 2024-2025 Sweden Connect
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
package se.digg.oidfed.service.trustanchor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.digg.oidfed.common.exception.InvalidIssuerException;
import se.digg.oidfed.common.exception.InvalidRequestException;
import se.digg.oidfed.common.exception.NotFoundException;
import se.digg.oidfed.service.ApplicationModule;
import se.digg.oidfed.service.submodule.TrustAnchorRepository;
import se.digg.oidfed.trustanchor.EntityStatementRequest;
import se.digg.oidfed.trustanchor.SubordinateListingRequest;
import se.digg.oidfed.trustanchor.TrustAnchor;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Controller for trust anchor.
 *
 * @author Felix Hellman
 */
@RestController
public class TrustAnchorController implements ApplicationModule {

  private final TrustAnchorRepository repository;

  /**
   * Constructor.
   *
   * @param repository with trust anchor submodules
   */
  public TrustAnchorController(final TrustAnchorRepository repository) {
    this.repository = repository;
  }

  /**
   * Retrieves the Entity Statement for a subject.
   * Note that the issuer parameter is defined in the standard API, but it is not supported if provided. This service
   * does not support the provision of Entity Statements for other issuers.
   *
   * @param alias the path of the issuing federation entity
   * @param subject the subject of the entity statement
   * @return the Entity Statement or error response
   * @throws NotFoundException if a given module is not found
   * @throws InvalidRequestException If there is a problem with incoming request parameters
   * @throws InvalidIssuerException If issuer is not found.
   */
  @GetMapping(value = "/{alias}/fetch", produces = "application/entity-statement+jwt")
  public String fetchEntityStatement(@PathVariable(name = "alias") final String alias,
      @RequestParam(name = "sub", required = false) final String subject)
      throws NotFoundException, InvalidRequestException, InvalidIssuerException {
    final TrustAnchor trustAnchor = this.repository.getTrustAnchor(alias)
            .orElseThrow(() ->
                new NotFoundException("Could not find given trust anchor for alias:'%s'".formatted(alias)));
    final EntityStatementRequest request =
        new EntityStatementRequest(URLDecoder.decode(subject, Charset.defaultCharset()));
    return trustAnchor.fetchEntityStatement(request);
  }

  /**
   * Retrieves the subordinate listing for provided entity type, trust mark status, trust mark ID, and intermediate
   * status.
   *
   * @param alias the path of the entity providing the response.
   * @param entityType the type of the entity (optional)
   * @param trustMarked the trust mark status of the entity (optional)
   * @param trustMarkId the ID of the trust mark (optional)
   * @param intermediate the intermediate status of the subordinate entities (optional)
   * @return the subordinate listing or an error response
   * @throws NotFoundException if a given module is not found
   */
  @GetMapping(value = "/{alias}/subordinate_listing", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> subordinateListing(
      @PathVariable(name = "alias") final String alias,
      @RequestParam(name = "entity_type", required = false) final String entityType,
      @RequestParam(name = "trust_marked", required = false) final Boolean trustMarked,
      @RequestParam(name = "trust_mark_id", required = false) final String trustMarkId,
      @RequestParam(name = "intermediate", required = false) final Boolean intermediate
  ) throws NotFoundException {
    final TrustAnchor trustAnchor = this.repository.getTrustAnchor(alias)
        .orElseThrow(() -> new NotFoundException("Could not find given trust anchor"));
    return trustAnchor.subordinateListing(
        new SubordinateListingRequest(alias, entityType, trustMarked, trustMarkId, intermediate));
  }

  @Override
  public String getModuleName() {
    return "TrustAnchor";
  }
}
