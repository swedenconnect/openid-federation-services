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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.tree.EntityStatementClaims;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds scraped subordinate entity statements for an intermediate entity.
 *
 * @param listing      the subordinate listing, null for data stored before the listing was kept
 * @param subordinates map of subject entity ID to signed subordinate statement
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
public record ScrapedIntermediate(List<String> listing, Map<String, SignedJWT> subordinates) {

  private static final ExecutorService FETCH_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Constructor for an intermediate whose listing holds the subjects of the given statements.
   *
   * @param subordinates map of subject entity ID to signed subordinate statement
   */
  public ScrapedIntermediate(final Map<String, SignedJWT> subordinates) {
    this(new ArrayList<>(subordinates.keySet()), subordinates);
  }

  /**
   * Scrapes the subordinate listing and the Subordinate Statements of an intermediate. When the listing cannot be
   * fetched, the previous listing is used. When a Subordinate Statement cannot be fetched, the previous statement is
   * used while its {@code exp} has not passed, otherwise the subordinate is left out. A failure for one subordinate
   * does not affect the others.
   *
   * @param entityId the entity identifier of the intermediate
   * @param client   federation client to use
   * @param metadata metadata for the intermediate endpoint
   * @param previous the intermediate in the previous snapshot, or null
   * @param listener receives the failed fetches
   * @return the scraped intermediate, or null if the listing could not be fetched and there is no previous listing
   */
  public static ScrapedIntermediate scrape(final String entityId, final FederationClient client,
      final Map<String, Object> metadata, final ScrapedIntermediate previous, final ScrapeFailureListener listener) {
    List<String> listing;
    try {
      listing = client.subordinateListing(
          new FederationRequest<>(SubordinateListingRequest.requestAll(), metadata));
      log.debug("Fetched subordinate listing of {} with {} entries", entityId, listing.size());
    }
    catch (final Exception e) {
      final List<String> previousListing = previous != null ? previous.previousListing() : null;
      listener.failed(ScrapeFailureListener.Part.SUBORDINATE_LISTING, entityId, ScrapedEntity.reason(e),
          previousListing != null);
      if (previousListing == null) {
        return null;
      }
      listing = previousListing;
    }
    final List<CompletableFuture<SignedJWT>> futures = listing.stream()
        .map(sub -> CompletableFuture.supplyAsync(
            () -> fetchStatement(sub, client, metadata, previous, listener), FETCH_EXECUTOR))
        .toList();
    final Map<String, SignedJWT> subordinates = new ConcurrentHashMap<>();
    futures.stream()
        .map(CompletableFuture::join)
        .filter(jwt -> jwt != null)
        .forEach(jwt -> subordinates.put(EntityStatementClaims.getEntityID(jwt).getValue(), jwt));
    return new ScrapedIntermediate(List.copyOf(listing), subordinates);
  }

  /**
   * Fetches the Subordinate Statement about a subordinate, falling back to the previous statement while it is
   * valid.
   *
   * @param sub      the subordinate
   * @param client   federation client to use
   * @param metadata metadata for the intermediate endpoint
   * @param previous the intermediate in the previous snapshot, or null
   * @param listener receives the failed fetches
   * @return the statement, or null if it could not be fetched and there is no valid previous statement
   */
  private static SignedJWT fetchStatement(final String sub, final FederationClient client,
      final Map<String, Object> metadata, final ScrapedIntermediate previous, final ScrapeFailureListener listener) {
    log.debug("Resolving subordinate {}", sub);
    try {
      return client.fetch(new FederationRequest<>(new FetchRequest(sub), metadata));
    }
    catch (final Exception e) {
      final SignedJWT old = previous != null && previous.subordinates() != null
          ? previous.subordinates().get(sub)
          : null;
      final Instant exp = old != null ? ScrapedEntity.expirationTime(old) : null;
      final boolean previousUsed = exp != null && exp.isAfter(Instant.now());
      listener.failed(ScrapeFailureListener.Part.SUBORDINATE_STATEMENT, sub, ScrapedEntity.reason(e), previousUsed);
      return previousUsed ? old : null;
    }
  }

  /**
   * Gives the listing to use when the listing cannot be fetched. Data stored before the listing was kept has only
   * the subjects of its statements.
   *
   * @return the listing, or null if there is none
   */
  private List<String> previousListing() {
    if (this.listing != null) {
      return this.listing;
    }
    return this.subordinates != null ? new ArrayList<>(this.subordinates.keySet()) : null;
  }
}
