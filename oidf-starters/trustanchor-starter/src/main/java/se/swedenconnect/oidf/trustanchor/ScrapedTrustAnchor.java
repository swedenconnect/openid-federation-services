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
package se.swedenconnect.oidf.trustanchor;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.exception.FederationException;
import se.swedenconnect.oidf.common.entity.exception.InvalidIssuerException;
import se.swedenconnect.oidf.common.entity.exception.NotFoundException;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.List;


/**
 * Trust anchor implementation backed by a scraped entity.
 *
 * @author Felix Hellman
 */
public class ScrapedTrustAnchor implements TrustAnchor {

  /**
   * Constructor.
   *
   * @param scrapedEntity the scraped entity to back this trust anchor
   */
  public ScrapedTrustAnchor(final ScrapedEntity scrapedEntity) {
    this.scrapedEntity = scrapedEntity;
  }

  private final ScrapedEntity scrapedEntity;

  @Override
  public EntityID getEntityId() {
    return this.scrapedEntity.getEntityID();
  }

  @Override
  public List<String> subordinateListing(final SubordinateListingRequest request) throws FederationException {
    return this.scrapedEntity.getIntermediate().subordinates().keySet().stream().toList();
  }

  @Override
  public String fetchEntityStatement(final FetchRequest request) throws InvalidIssuerException, NotFoundException {
    final com.nimbusds.jwt.SignedJWT jwt =
        this.scrapedEntity.getIntermediate().subordinates().get(request.subject());
    if (jwt == null) {
      throw new NotFoundException("No entity statement found for subject: " + request.subject());
    }
    return jwt.serialize();
  }
}
