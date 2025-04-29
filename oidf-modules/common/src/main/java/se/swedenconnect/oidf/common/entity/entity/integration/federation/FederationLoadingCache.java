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
package se.swedenconnect.oidf.common.entity.entity.integration.federation;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import se.swedenconnect.oidf.common.entity.entity.integration.Cache;
import se.swedenconnect.oidf.common.entity.entity.integration.Expirable;

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

  private final Cache<String, EntityStatement> entityConfigurationCache;
  private final Cache<String, EntityStatement> fetchCache;
  private final Cache<String, List<String>> subordinateListingCache;
  private final Cache<String, SignedJWT> trustMarkCache;
  private final Cache<String, SignedJWT> resolveCache;
  private final Cache<String, List<String>> trustMarkListingCache;

  /**
   * Constructor.
   *
   * @param client                   for making requests
   * @param entityConfigurationCache for storing entity configuration
   * @param fetchCache               for storing entity statements
   * @param subordinateListingCache  for storing subordinate listings
   * @param trustMarkCache           for storing trust marks
   * @param resolveCache             for storing resolver responses
   * @param trustMarkListingCache    for storing trust mark listings
   */
  public FederationLoadingCache(
      final FederationClient client,
      final Cache<String, EntityStatement> entityConfigurationCache,
      final Cache<String, EntityStatement> fetchCache,
      final Cache<String, List<String>> subordinateListingCache,
      final Cache<String, SignedJWT> trustMarkCache,
      final Cache<String, SignedJWT> resolveCache,
      final Cache<String, List<String>> trustMarkListingCache) {

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
    if (!request.useCachedValue() || this.entityConfigurationCache.shouldRefresh(request.toString())) {
      final EntityStatement entityStatement = this.client.entityConfiguration(request);
      final Instant expiration = entityStatement.getClaimsSet().getExpirationTime().toInstant();
      final Instant iat = entityStatement.getClaimsSet().getIssueTime().toInstant();
      this.entityConfigurationCache.add(request.toString(), new Expirable<>(expiration, iat, entityStatement));
      return entityStatement;
    }
    return this.entityConfigurationCache.get(request.toString());
  }

  @Override
  public EntityStatement fetch(final FederationRequest<FetchRequest> request) {
    if (!request.useCachedValue() || this.fetchCache.shouldRefresh(request.toString())) {
      final EntityStatement entityStatement = this.client.fetch(request);
      final Instant expiration = entityStatement.getClaimsSet().getExpirationTime().toInstant();
      final Instant iat = entityStatement.getClaimsSet().getIssueTime().toInstant();
      this.fetchCache.add(request.toString(), new Expirable<>(expiration, iat, entityStatement));
      return entityStatement;
    }
    return this.fetchCache.get(request.toString());
  }

  @Override
  public List<String> subordinateListing(final FederationRequest<SubordinateListingRequest> request) {
    if (!request.useCachedValue() || this.subordinateListingCache.shouldRefresh(request.toString())) {
      final List<String> subordinateListing = this.client.subordinateListing(request);
      final Expirable<List<String>> expirable =
          new Expirable<>(Instant.now().plus(Duration.ofSeconds(30)), Instant.now(), subordinateListing);
      this.subordinateListingCache.add(request.toString(), expirable);
      return subordinateListing;
    }
    return this.subordinateListingCache.get(request.toString());
  }

  @Override
  public SignedJWT trustMark(final FederationRequest<TrustMarkRequest> request) {
    if (!request.useCachedValue() || this.trustMarkCache.shouldRefresh(request.toString())) {
      final SignedJWT trustMark = this.client.trustMark(request);
      this.trustMarkCache.add(request.toString(), new Expirable<>(getExpirationFromJwt(trustMark), Instant.now(),
          trustMark));
      return trustMark;
    }
    return this.trustMarkCache.get(request.toString());
  }

  @Override
  public SignedJWT resolve(final FederationRequest<ResolveRequest> request) {
    if (!request.useCachedValue() || this.resolveCache.shouldRefresh(request.toString())) {
      final SignedJWT resolseResponse = this.client.resolve(request);
      this.resolveCache.add(request.toString(),
          new Expirable<>(getExpirationFromJwt(resolseResponse), Instant.now(), resolseResponse)
      );
      return resolseResponse;
    }
    return this.resolveCache.get(request.toString());
  }

  @Override
  public List<String> trustMarkedListing(final FederationRequest<TrustMarkListingRequest> request) {
    if (!request.useCachedValue() || this.trustMarkListingCache.shouldRefresh(request.toString())) {
      final List<String> trustMarkedListing = this.client.trustMarkedListing(request);
      final Expirable<List<String>> expirable =
          new Expirable<>(Instant.now().plus(Duration.ofSeconds(30)), Instant.now(), trustMarkedListing);
      this.trustMarkListingCache.add(request.toString(), expirable);
      return trustMarkedListing;
    }
    return this.trustMarkListingCache.get(request.toString());
  }

  private static Instant getExpirationFromJwt(final SignedJWT trustMark) {
    try {
      return trustMark.getJWTClaimsSet().getExpirationTime().toInstant();
    } catch (final ParseException e) {
      return Instant.now().plus(1, ChronoUnit.DAYS);
    }
  }
}
