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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EcLocationValidator;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationTrustMarkStatusRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryResponseException;
import se.swedenconnect.oidf.common.entity.entity.integration.trustmark.TrustMarkStatusResponse;
import se.swedenconnect.oidf.common.entity.tree.scraping.FederationFetchException;
import se.swedenconnect.oidf.common.entity.tree.scraping.FetchError;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

/**
 * {@link RestClient} implementation of {@link FederationClient}.
 *
 * @author Felix Hellman
 * @author Martin Lindström
 */
@Slf4j
public class RestClientFederationClient implements FederationClient {

  /** Type reference for a response body holding a JSON array of strings. */
  private static final ParameterizedTypeReference<List<String>> STRING_LIST = new ParameterizedTypeReference<>() {
  };

  /** The client used for all requests. */
  private final @NonNull RestClient client;

  /** Registry for request metrics. */
  private final @NonNull MeterRegistry registry;

  /** Registry for per-cycle uptime tracking. */
  private final @NonNull UptimeRegistry uptimeRegistry;

  /** Validator for ec_location values. */
  private final @NonNull EcLocationValidator ecLocationValidator;

  /**
   * Constructor.
   *
   * @param client to use for requests
   * @param registry for metrics
   * @param uptimeRegistry for per-cycle uptime tracking
   * @param ecLocationValidator for checking ec_location values before they are used
   */
  public RestClientFederationClient(
      final @NonNull RestClient client,
      final @NonNull MeterRegistry registry,
      final @NonNull UptimeRegistry uptimeRegistry,
      final @NonNull EcLocationValidator ecLocationValidator) {
    this.client = client;
    this.registry = registry;
    this.uptimeRegistry = uptimeRegistry;
    this.ecLocationValidator = ecLocationValidator;
  }

  /**
   * Gets the Entity Configuration of an entity. If the request has an {@code ec_location}, the Entity
   * Configuration is read from that location, which may be a data URL. Otherwise it is fetched from
   * {@code /.well-known/openid-federation} under the entity identifier.
   *
   * @param request for entity configuration
   * @return entity configuration as a signed JWT
   * @throws FederationFetchException if the response has a non-2xx status
   */
  @Override
  public @NonNull SignedJWT entityConfiguration(
      final @NonNull FederationRequest<EntityConfigurationRequest> request) {
    final String entityId = request.parameters().entityID().getValue();
    final long startNanos = System.nanoTime();
    try {
      final String jwt = Optional.ofNullable(request.parameters().ecLocation())
          .map(location -> {
            this.ecLocationValidator.validate(location);
            if (EcLocationValidator.isDataUrl(location)) {
              return EcLocationValidator.getDataUrlContent(location);
            }
            // The ec_location may already be percent-encoded, so it is passed as a URI to avoid encoding it again
            return requireBody(this.client.get()
                .uri(URI.create(location))
                .retrieve()
                .body(String.class), location);
          })
          .orElseGet(
              () -> requireBody(this.client.mutate()
                  .baseUrl(entityId)
                  .build()
                  .get()
                  .uri(builder -> builder.path("/.well-known/openid-federation").build()).retrieve()
                  .body(String.class), entityId)
          );
      final SignedJWT result = SignedJWT.parse(jwt);
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", entityId),
          Tag.of("outcome", "success")
      )).increment();
      final long responseTimeMs = (System.nanoTime() - startNanos) / 1_000_000;
      this.uptimeRegistry.record(entityId, true);
      this.uptimeRegistry.recordSuccess(entityId, responseTimeMs);
      return result;
    }
    catch (final ParseException e) {
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", entityId),
          Tag.of("outcome", "failure")
      )).increment();
      this.uptimeRegistry.record(entityId, false);
      throw new RuntimeException(e);
    }
    catch (final Exception e) {
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", entityId),
          Tag.of("outcome", "failure")
      )).increment();
      this.uptimeRegistry.record(entityId, false);
      if (e instanceof final RestClientResponseException re) {
        throw new FederationFetchException(FetchError.httpStatus(re.getStatusCode().value()), e.getMessage(), e);
      }
      throw e;
    }
  }

  /**
   * Fetches a Subordinate Statement from the {@code federation_fetch_endpoint} of the request metadata.
   *
   * @param request for subordinate statement
   * @return entity statement with subordinate statement
   */
  @Override
  public @NonNull SignedJWT fetch(final @NonNull FederationRequest<FetchRequest> request) {
    final String url = getEndpoint(request, "federation_fetch_endpoint").orElseThrow();
    try {
      final String body = this.client.mutate().baseUrl(url).build()
          .get()
          .uri(builder -> builder
              .queryParam("sub", request.parameters()
                  .subject())
              .build())
          .retrieve()
          .body(String.class);
      return SignedJWT.parse(requireBody(body, url));
    }
    catch (final ParseException e) {
      log.error("Failed to parse entity statement for fetch request {}", request, e);
      throw new RegistryResponseException("Failed to fetch entity statement", e);
    }
    catch (final Exception e) {
      log.error("Failed to fetch entity statement for request {}", request, e);
      throw e;
    }
  }

  /**
   * Gets the subordinates from the {@code federation_list_endpoint} of the request metadata.
   *
   * @param request for subordinate listing
   * @return list of subordinates
   */
  @Override
  public @NonNull List<String> subordinateListing(final @NonNull FederationRequest<SubordinateListingRequest> request) {
    final String url = getEndpoint(request, "federation_list_endpoint").orElseThrow();
    try {
      return requireBody(this.client.mutate().baseUrl(url).build()
          .get()
          .retrieve()
          .body(STRING_LIST), url);
    }
    catch (final Exception e) {
      log.error("Failed to fetch subordinate listing for request {}", request, e);
      throw e;
    }
  }

  /**
   * Gets a trust mark from the {@code federation_trust_mark_endpoint} of the request metadata, or from
   * {@code /trust_mark} under the trust mark issuer if the metadata has no such endpoint.
   *
   * @param request for trust mark
   * @return trust mark jwt
   */
  @Override
  public @NonNull SignedJWT trustMark(final @NonNull FederationRequest<TrustMarkRequest> request) {
    final String path = getEndpoint(request, "federation_trust_mark_endpoint")
        .orElseGet(() -> request.parameters().trustMarkIssuer().getValue() + "/trust_mark");
    final String body = this.client.mutate().baseUrl(path).build()
        .get()
        .uri(builder -> builder
            .queryParam("trust_mark_type", request.parameters().trustMarkType().getValue())
            .queryParam("sub", request.parameters().subject().getValue())
            .build())
        .retrieve()
        .body(String.class);
    try {
      return SignedJWT.parse(requireBody(body, path));
    }
    catch (final ParseException e) {
      throw new RegistryResponseException("Failed to fetch entity statement", e);
    }
  }

  /**
   * Not implemented, nothing in the service uses resolve internally.
   *
   * @param request for resolver
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  @Override
  public @NonNull SignedJWT resolve(final @NonNull FederationRequest<ResolveRequest> request) {
    throw new UnsupportedOperationException("Resolve requests are not implemented by RestClientFederationClient");
  }

  /**
   * Gets the trust marked entities from the {@code federation_trust_mark_list_endpoint} of the request metadata.
   *
   * @param request for trust marked listing
   * @return list of trust marked subjects
   */
  @Override
  public @NonNull List<String> trustMarkedListing(final @NonNull FederationRequest<TrustMarkListingRequest> request) {
    final String path = getEndpoint(request, "federation_trust_mark_list_endpoint").orElseThrow();
    return requireBody(this.client.mutate().baseUrl(path).build()
        .get()
        .uri(builder -> builder
            .queryParam("trust_mark_type", request.parameters().trustMarkType())
            .build())
        .retrieve()
        .body(STRING_LIST), path);
  }

  /**
   * Gets the status of a trust mark from the {@code federation_trust_mark_status_endpoint} of the request
   * metadata. If the metadata has no such endpoint, or the request fails, the response is marked as an error.
   *
   * @param request containing the trust mark JWT and issuer entity ID
   * @return the trust mark status response
   */
  @Override
  public @NonNull TrustMarkStatusResponse trustMarkStatus(
      final @NonNull FederationRequest<FederationTrustMarkStatusRequest> request) {
    final String path = getEndpoint(request, "federation_trust_mark_status_endpoint").orElse(null);
    if (path == null) {
      log.info("Trust mark issuer {} has no valid federation_trust_mark_status_endpoint",
          request.parameters().trustMarkIssuer());
      return new TrustMarkStatusResponse(null, true);
    }
    try {
      final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("trust_mark", request.parameters().trustMarkJwt());
      final String body = this.client.mutate().baseUrl(path).build()
          .post()
          .uri(UriBuilder::build)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(String.class);
      return new TrustMarkStatusResponse(SignedJWT.parse(requireBody(body, path)), false);
    }
    catch (final Exception e) {
      log.warn("Failed to get trust mark status from '{}': {}", path, e.getMessage());
      log.debug("Trust mark status request {} failed", request, e);
      return new TrustMarkStatusResponse(null, true);
    }
  }

  /**
   * Gets an endpoint URL from the federation entity metadata of a request.
   *
   * @param request the request holding the metadata
   * @param name the metadata parameter name of the endpoint
   * @return the endpoint URL, or an empty optional if the metadata has no such string value
   */
  private static @NonNull Optional<String> getEndpoint(
      final @NonNull FederationRequest<?> request, final @NonNull String name) {
    return Optional.ofNullable(request.federationEntityMetadata().get(name))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }

  /**
   * Checks that a response has a body.
   *
   * @param body the response body
   * @param url the URL the response came from
   * @param <T> the body type
   * @return the body
   * @throws RestClientException if the response has no body
   */
  private static <T> @NonNull T requireBody(final @Nullable T body, final @NonNull String url) {
    if (body == null) {
      throw new RestClientException("Empty response from '%s'".formatted(url));
    }
    return body;
  }
}
