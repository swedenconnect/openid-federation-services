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
package se.digg.oidfed.common.entity.integration.federation;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import se.digg.oidfed.common.entity.integration.Cache;
import se.digg.oidfed.common.entity.integration.Expirable;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Loading cache for federation requests.
 *
 * @author Felix Hellman
 */
public class FederationLoadingCache implements FederationClient {

  private final FederationClient client;

  private final Cache<FederationRequest<EntityConfigurationRequest>, EntityStatement> entityConfigurationCache;
  private final Cache<FederationRequest<FetchRequest>, EntityStatement> fetchCache;
  private final Cache<FederationRequest<SubordinateListingRequest>, List<String>> subordinateListingCache;
  private final Cache<FederationRequest<TrustMarkRequest>, SignedJWT> trustMarkCache;
  private final Cache<FederationRequest<ResolveRequest>, SignedJWT> resolveCache;
  private final Cache<FederationRequest<TrustMarkListingRequest>, List<String>> trustMarkListingCache;

  /**
   * Constructor.
   * @param client for making requests
   * @param entityConfigurationCache for storing entity configuration
   * @param fetchCache for storing entity statements
   * @param subordinateListingCache for storing subordinate listings
   * @param trustMarkCache for storing trust marks
   * @param resolveCache for storing resolver responses
   * @param trustMarkListingCache for storing trust mark listings
   */
  public FederationLoadingCache(
      final FederationClient client,
      final Cache<FederationRequest<EntityConfigurationRequest>, EntityStatement> entityConfigurationCache,
      final Cache<FederationRequest<FetchRequest>, EntityStatement> fetchCache,
      final Cache<FederationRequest<SubordinateListingRequest>, List<String>> subordinateListingCache,
      final Cache<FederationRequest<TrustMarkRequest>, SignedJWT> trustMarkCache,
      final Cache<FederationRequest<ResolveRequest>, SignedJWT> resolveCache,
      final Cache<FederationRequest<TrustMarkListingRequest>, List<String>> trustMarkListingCache) {

    this.entityConfigurationCache = entityConfigurationCache;
    this.subordinateListingCache = subordinateListingCache;
    this.trustMarkListingCache = trustMarkListingCache;
    this.trustMarkCache = trustMarkCache;
    this.resolveCache = resolveCache;
    this.fetchCache = fetchCache;
    this.client = client;
  }

  @Override
  public EntityStatement entityConfiguration(final FederationRequest<EntityConfigurationRequest> request) {
    if (!request.useCachedValue() || this.entityConfigurationCache.shouldRefresh(request)) {
      final EntityStatement entityStatement = this.client.entityConfiguration(request);
      final Instant expiration = entityStatement.getClaimsSet().getExpirationTime().toInstant();
      this.entityConfigurationCache.add(request, new Expirable<>(expiration, entityStatement));
      return entityStatement;
    }
    return this.entityConfigurationCache.get(request);
  }

  @Override
  public EntityStatement fetch(final FederationRequest<FetchRequest> request) {
    if (!request.useCachedValue() || this.fetchCache.shouldRefresh(request)) {
      final EntityStatement entityStatement = this.client.fetch(request);
      final Instant expiration = entityStatement.getClaimsSet().getExpirationTime().toInstant();
      this.fetchCache.add(request, new Expirable<>(expiration, entityStatement));
      return entityStatement;
    }
    return this.fetchCache.get(request);
  }

  @Override
  public List<String> subordinateListing(final FederationRequest<SubordinateListingRequest> request) {
    if (!request.useCachedValue() || this.subordinateListingCache.shouldRefresh(request)) {
      final List<String> subordinateListing = this.client.subordinateListing(request);
      final Expirable<List<String>> expirable =
          new Expirable<>(Instant.now().plus(Duration.ofHours(1)), subordinateListing);
      this.subordinateListingCache.add(request, expirable);
      return subordinateListing;
    }
    return this.subordinateListingCache.get(request);
  }

  @Override
  public SignedJWT trustMark(final FederationRequest<TrustMarkRequest> request) {
    if (!request.useCachedValue() || this.trustMarkCache.shouldRefresh(request)) {
      final SignedJWT trustMark = this.client.trustMark(request);
      this.trustMarkCache.add(request, new Expirable<>(getExpirationFromJwt(trustMark), trustMark));
      return trustMark;
    }
    return this.trustMarkCache.get(request);
  }

  @Override
  public SignedJWT resolve(final FederationRequest<ResolveRequest> request) {
    if (!request.useCachedValue() || this.resolveCache.shouldRefresh(request)) {
      final SignedJWT resolseResponse = this.client.resolve(request);
      this.resolveCache.add(request, new Expirable<>(getExpirationFromJwt(resolseResponse), resolseResponse));
      return resolseResponse;
    }
    return this.resolveCache.get(request);
  }

  @Override
  public List<String> trustMarkedListing(final FederationRequest<TrustMarkListingRequest> request) {
    if (!request.useCachedValue() || this.trustMarkListingCache.shouldRefresh(request)) {
      final List<String> trustMarkedListing = this.client.trustMarkedListing(request);
      final Expirable<List<String>> expirable =
          new Expirable<>(Instant.now().plus(Duration.ofDays(1)), trustMarkedListing);
      this.trustMarkListingCache.add(request, expirable);
      return trustMarkedListing;
    }
    return this.trustMarkListingCache.get(request);
  }

  private static Instant getExpirationFromJwt(final SignedJWT trustMark) {
    try {
      return trustMark.getJWTClaimsSet().getExpirationTime().toInstant();
    } catch (final ParseException e) {
      return Instant.now().plus(1, ChronoUnit.DAYS);
    }
  }
}
