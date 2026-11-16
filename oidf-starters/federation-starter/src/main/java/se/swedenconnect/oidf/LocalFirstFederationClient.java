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

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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

import java.util.List;
import java.util.Optional;

/**
 * {@link FederationClient} that answers requests to endpoints hosted by this service in-process, and sends all
 * other requests to its delegate over HTTP. The resolver then builds the statements of its own Trust Anchors and
 * Intermediates directly, instead of calling itself over HTTP.
 *
 * <p>Answered in-process: Entity Configurations, Subordinate Statements, subordinate listings and trust mark
 * statuses of the endpoints a {@link LocalFederationEndpoints} hosts. These are built the same way as the responses of the endpoints, but
 * they are not read from or written to the response caches.</p>
 *
 * @author Felix Hellman
 */
@Slf4j
public class LocalFirstFederationClient implements FederationClient {

  private final FederationClient delegate;
  private final ObjectProvider<LocalFederationEndpoints> localEndpoints;
  private final UptimeRegistry uptimeRegistry;

  /**
   * Constructor.
   *
   * @param delegate       for requests to endpoints not hosted by this service
   * @param localEndpoints the endpoints hosted by this service
   * @param uptimeRegistry to record Entity Configurations answered in-process in, as the delegate does
   */
  public LocalFirstFederationClient(
      final FederationClient delegate,
      final ObjectProvider<LocalFederationEndpoints> localEndpoints,
      final UptimeRegistry uptimeRegistry) {

    this.delegate = delegate;
    this.localEndpoints = localEndpoints;
    this.uptimeRegistry = uptimeRegistry;
  }

  @Override
  public SignedJWT entityConfiguration(final FederationRequest<EntityConfigurationRequest> request) {
    final String entityId = request.parameters().entityID().getValue();
    final long startNanos = System.nanoTime();
    final Optional<SignedJWT> local = this.localEndpoints.stream()
        .map(hosted -> localEntityConfiguration(hosted, entityId))
        .flatMap(Optional::stream)
        .findFirst();
    if (local.isEmpty()) {
      return this.delegate.entityConfiguration(request);
    }
    this.uptimeRegistry.record(entityId, true);
    this.uptimeRegistry.recordSuccess(entityId, (System.nanoTime() - startNanos) / 1_000_000);
    return local.get();
  }

  private static Optional<SignedJWT> localEntityConfiguration(
      final LocalFederationEndpoints local, final String entityId) {
    try {
      final Optional<String> entityConfiguration = local.entityConfiguration(entityId);
      if (entityConfiguration.isPresent()) {
        return Optional.of(SignedJWT.parse(entityConfiguration.get()));
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new IllegalStateException("Entity Configuration of %s failed".formatted(entityId), e);
    }
  }

  @Override
  public SignedJWT fetch(final FederationRequest<FetchRequest> request) {
    final Optional<String> endpoint = endpoint(request, "federation_fetch_endpoint");
    return endpoint.flatMap(fetchEndpoint -> this.localEndpoints.stream()
            .map(local -> localFetch(local, fetchEndpoint, request.parameters().subject()))
            .flatMap(Optional::stream)
            .findFirst())
        .orElseGet(() -> this.delegate.fetch(request));
  }

  private static Optional<SignedJWT> localFetch(
      final LocalFederationEndpoints local, final String fetchEndpoint, final String subject) {
    try {
      final Optional<String> statement = local.fetch(fetchEndpoint, subject);
      if (statement.isPresent()) {
        return Optional.of(SignedJWT.parse(statement.get()));
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new IllegalStateException("Fetch of %s from %s failed".formatted(subject, fetchEndpoint), e);
    }
  }

  @Override
  public List<String> subordinateListing(final FederationRequest<SubordinateListingRequest> request) {
    final Optional<String> endpoint = endpoint(request, "federation_list_endpoint");
    return endpoint.flatMap(listEndpoint -> this.localEndpoints.stream()
            .map(local -> localSubordinateListing(local, listEndpoint, request.parameters()))
            .flatMap(Optional::stream)
            .findFirst())
        .orElseGet(() -> this.delegate.subordinateListing(request));
  }

  private static Optional<List<String>> localSubordinateListing(
      final LocalFederationEndpoints local, final String listEndpoint, final SubordinateListingRequest request) {
    try {
      return local.subordinateListing(listEndpoint, request);
    } catch (final Exception e) {
      throw new IllegalStateException("Subordinate listing of %s failed".formatted(listEndpoint), e);
    }
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
    final String issuerId = request.parameters().trustMarkIssuer();
    for (final LocalFederationEndpoints local : this.localEndpoints) {
      try {
        final Optional<String> status = local.trustMarkStatus(issuerId, request.parameters().trustMarkJwt());
        if (status.isPresent()) {
          return new TrustMarkStatusResponse(SignedJWT.parse(status.get()), false);
        }
      } catch (final Exception e) {
        log.warn("Failed to get trust mark status from '{}': {}", issuerId, e.getMessage());
        return new TrustMarkStatusResponse(null, true);
      }
    }
    return this.delegate.trustMarkStatus(request);
  }

  private static Optional<String> endpoint(final FederationRequest<?> request, final String name) {
    return Optional.ofNullable(request.federationEntityMetadata())
        .map(metadata -> metadata.get(name))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }
}
