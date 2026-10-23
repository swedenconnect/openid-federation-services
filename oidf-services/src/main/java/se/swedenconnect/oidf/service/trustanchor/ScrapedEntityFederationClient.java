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
package se.swedenconnect.oidf.service.trustanchor;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.CacheSnapshot;
import se.swedenconnect.oidf.common.entity.tree.NodeKey;
import se.swedenconnect.oidf.common.entity.tree.scraping.ScrapedEntity;

import java.util.List;

/**
 * Federation client that answers entity configuration requests from the scraped entities of a single resolver tree
 * snapshot, otherwise delegate.
 *
 * @author Felix Hellman
 */
@Slf4j
public class ScrapedEntityFederationClient implements FederationClient {

  private final CacheSnapshot<ScrapedEntity> snapshot;
  private final FederationClient delegate;

  /**
   * Constructor.
   *
   * @param snapshot of the resolver tree to read scraped entities from
   * @param delegate client to use when no scraped entity is available
   */
  public ScrapedEntityFederationClient(final CacheSnapshot<ScrapedEntity> snapshot, final FederationClient delegate) {
    this.snapshot = snapshot;
    this.delegate = delegate;
  }

  @Override
  public SignedJWT entityConfiguration(final FederationRequest<EntityConfigurationRequest> request) {
    final String entityId = request.parameters().entityID().getValue();
    final ScrapedEntity scraped = this.snapshot.getData(new NodeKey(entityId));
    if (scraped == null || scraped.getEntityStatement() == null || scraped.getScrapeFailedAt() != null) {
      log.debug("No scraped entity configuration for {}, fetching it", entityId);
      return this.delegate.entityConfiguration(request);
    }
    return scraped.getEntityStatement();
  }

  @Override
  public SignedJWT fetch(final FederationRequest<FetchRequest> request) {
    return this.delegate.fetch(request);
  }

  @Override
  public List<String> subordinateListing(final FederationRequest<SubordinateListingRequest> request) {
    return this.delegate.subordinateListing(request);
  }

  @Override
  public SignedJWT trustMark(final FederationRequest<TrustMarkRequest> request) {
    return this.delegate.trustMark(request);
  }

  @Override
  public SignedJWT resolve(final FederationRequest<ResolveRequest> request) {
    return this.delegate.resolve(request);
  }

  @Override
  public List<String> trustMarkedListing(final FederationRequest<TrustMarkListingRequest> request) {
    return this.delegate.trustMarkedListing(request);
  }

  @Override
  public TrustMarkStatusResponse trustMarkStatus(final FederationRequest<FederationTrustMarkStatusRequest> request) {
    return this.delegate.trustMarkStatus(request);
  }
}
