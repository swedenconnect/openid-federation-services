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
package se.swedenconnect.oidf.common.entity.tree.scraping;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;

import java.util.List;
import java.util.Map;

/**
 * Holds scraped subordinate entity statements for an intermediate entity.
 *
 * @param subordinates map of subject entity ID to signed subordinate statement
 * @author Felix Hellman
 */
@Slf4j
public record ScrapedIntermediate(Map<String, SignedJWT> subordinates) {

  /**
   * Scrapes all subordinates of this intermediate from the federation.
   *
   * @param client   federation client to use
   * @param metadata metadata for the intermediate endpoint
   */
  public void scrape(final FederationClient client, final Map<String, Object> metadata) {
    final List<String> subordinates = client.subordinateListing(
        new FederationRequest<>(SubordinateListingRequest.requestAll(), metadata));
    subordinates.forEach(sub -> {
      log.debug("Resolving subordinate {}", sub);
      final EntityStatement fetch = client.fetch(new FederationRequest<>(new FetchRequest(sub), metadata));
      this.subordinates.put(sub, fetch.getSignedStatement());
    });
  }
}
