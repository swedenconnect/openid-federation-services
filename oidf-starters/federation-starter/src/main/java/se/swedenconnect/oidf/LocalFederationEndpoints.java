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
package se.swedenconnect.oidf;

import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;

import java.util.List;
import java.util.Optional;

/**
 * Federation endpoints that this service hosts itself, so that {@link LocalFirstFederationClient} can answer a
 * request to them without an HTTP call. Implemented by the modules that serve the endpoints.
 *
 * @author Felix Hellman
 */
public interface LocalFederationEndpoints {

  /**
   * @param entityId the entity whose Entity Configuration is requested
   * @return the Entity Configuration, or empty if this service does not host the entity
   * @throws Exception if the entity is hosted here but the Entity Configuration cannot be issued
   */
  default Optional<String> entityConfiguration(final String entityId) throws Exception {
    return Optional.empty();
  }

  /**
   * @param fetchEndpoint the {@code federation_fetch_endpoint} the request is for
   * @param subject       of the Subordinate Statement
   * @return the Subordinate Statement, or empty if this service does not host the endpoint
   * @throws Exception if the endpoint is hosted here but the statement cannot be issued
   */
  default Optional<String> fetch(final String fetchEndpoint, final String subject) throws Exception {
    return Optional.empty();
  }

  /**
   * @param listEndpoint the {@code federation_list_endpoint} the request is for
   * @param request      the listing parameters
   * @return the subordinates, or empty if this service does not host the endpoint
   * @throws Exception if the endpoint is hosted here but the listing cannot be made
   */
  default Optional<List<String>> subordinateListing(
      final String listEndpoint, final SubordinateListingRequest request) throws Exception {
    return Optional.empty();
  }

  /**
   * @param issuerId  the Trust Mark Issuer the status is requested from
   * @param trustMark the trust mark JWT to give the status of
   * @return the trust mark status response JWT, or empty if this service does not host the issuer
   * @throws Exception if the issuer is hosted here but the status cannot be given
   */
  default Optional<String> trustMarkStatus(final String issuerId, final String trustMark) throws Exception {
    return Optional.empty();
  }
}
