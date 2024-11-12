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
package se.digg.oidfed.service.trustanchor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.digg.oidfed.service.ApplicationModule;
import se.digg.oidfed.trustanchor.EntityStatementRequest;
import se.digg.oidfed.trustanchor.SubordinateListingRequest;
import se.digg.oidfed.trustanchor.TrustAnchor;

import java.util.List;

/**
 * Controller for trust anchor.
 *
 * @author Felix Hellman
 */
@RestController
@ConditionalOnProperty(value = TrustAnchorConfigurationProperties.PROPERTY_PATH + ".active", havingValue = "true")
public class TrustAnchorController implements ApplicationModule {

  private final TrustAnchor trustAnchor;

  /**
   * Constructor.
   *
   * @param trustAnchor
   */
  public TrustAnchorController(final TrustAnchor trustAnchor) {
    this.trustAnchor = trustAnchor;
  }

  /**
   * Retrieves the Entity Statement for a subject.
   * Note that the issuer parameter is defined in the standard API, but it is not supported if provided. This service
   * does not support the provision of Entity Statements for other issuers.
   *
   * @param name the name of the issuing federation entity
   * @param issuer the issuer of the entity statement (not supported)
   * @param subject the subject of the entity statement
   * @return the Entity Statement or error response
   */
  @GetMapping(value = "/{name}/fetch", produces = "application/entity-statement+jwt")
  public String fetchEntityStatement(@PathVariable(name = "name") String name,
      @RequestParam(name = "iss", required = false) String issuer,
      @RequestParam(name = "sub", required = false) String subject) {
    return trustAnchor.fetchEntityStatement(new EntityStatementRequest(name, issuer, subject));
  }

  /**
   * Retrieves the subordinate listing for provided entity type, trust mark status, trust mark ID, and intermediate
   * status.
   *
   * @param name the name of the entity providing the response.
   * @param entityType the type of the entity (optional)
   * @param trustMarked the trust mark status of the entity (optional)
   * @param trustMarkId the ID of the trust mark (optional)
   * @param intermediate the intermediate status of the subordinate entities (optional)
   * @return the subordinate listing or an error response
   */
  @GetMapping(value = "/{name}/subordinate_listing", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> subordinateListing(@PathVariable(name = "name") String name,
      @RequestParam(name = "entity_type", required = false) String entityType,
      @RequestParam(name = "trust_marked", required = false) Boolean trustMarked,
      @RequestParam(name = "trust_mark_id", required = false) String trustMarkId,
      @RequestParam(name = "intermediate", required = false) Boolean intermediate
  ) {
    return trustAnchor.subordinateListing(
        new SubordinateListingRequest(name, entityType, trustMarked, trustMarkId, intermediate));
  }

  @Override
  public String getModuleName() {
    return "TrustAnchor";
  }
}
