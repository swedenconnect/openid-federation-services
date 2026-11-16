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

import se.swedenconnect.oidf.LocalFederationEndpoints;
import se.swedenconnect.oidf.common.entity.entity.integration.CompositeRecordSource;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.records.EntityRecord;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The fetch and subordinate listing endpoints of the Trust Anchors and Intermediates hosted by this service,
 * answered the same way as {@code TrustAnchorRouter} answers them over HTTP.
 *
 * @author Felix Hellman
 */
public class TrustAnchorLocalEndpoints implements LocalFederationEndpoints {

  private final CompositeRecordSource source;
  private final TrustAnchorFactory trustAnchorFactory;

  /**
   * Constructor.
   *
   * @param source             of the hosted entities and Trust Anchor properties
   * @param trustAnchorFactory for the Trust Anchors
   */
  public TrustAnchorLocalEndpoints(final CompositeRecordSource source, final TrustAnchorFactory trustAnchorFactory) {
    this.source = source;
    this.trustAnchorFactory = trustAnchorFactory;
  }

  @Override
  public Optional<String> fetch(final String fetchEndpoint, final String subject) throws Exception {
    final Optional<TrustAnchor> trustAnchor = this.trustAnchor(fetchEndpoint, EntityRecord::getFederationFetchEndpoint);
    if (trustAnchor.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(trustAnchor.get().fetchEntityStatement(new FetchRequest(subject)));
  }

  @Override
  public Optional<List<String>> subordinateListing(final String listEndpoint, final SubordinateListingRequest request)
      throws Exception {
    final Optional<TrustAnchor> trustAnchor = this.trustAnchor(listEndpoint, EntityRecord::getFederationListEndpoint);
    if (trustAnchor.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(trustAnchor.get().subordinateListing(request));
  }

  /**
   * Finds the hosted Trust Anchor or Intermediate that publishes an endpoint.
   *
   * @param endpoint the requested endpoint
   * @param published the endpoint an entity publishes
   * @return the Trust Anchor, or empty if no hosted Trust Anchor or Intermediate publishes the endpoint
   */
  private Optional<TrustAnchor> trustAnchor(
      final String endpoint, final Function<EntityRecord, Optional<String>> published) {

    return this.source.getAllEntities().stream()
        .filter(entity -> published.apply(entity).map(endpoint::equalsIgnoreCase).orElse(false))
        .findFirst()
        .flatMap(entity -> this.source.getTrustAnchorProperties().stream()
            .filter(properties -> properties.getEntityIdentifier().getValue()
                .equals(entity.getEntityIdentifier().getValue()))
            .findFirst())
        .map(this.trustAnchorFactory::create);
  }
}
