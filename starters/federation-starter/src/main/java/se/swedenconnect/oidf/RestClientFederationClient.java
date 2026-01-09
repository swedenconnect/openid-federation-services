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
package se.swedenconnect.oidf;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.EntityConfigurationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationClient;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FederationRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.FetchRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.ResolveRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.SubordinateListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkListingRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.federation.TrustMarkRequest;
import se.swedenconnect.oidf.common.entity.entity.integration.registry.RegistryResponseException;

import java.util.List;
import java.util.Optional;

/**
 * {@link RestClient} implementation of {@link FederationClient}.
 *
 * @author Felix Hellman
 */
public class RestClientFederationClient implements FederationClient {
  private final RestClient client;
  private final MeterRegistry registry;

  /**
   * @param client to use for requests
   * @param registry for metrics
   */
  public RestClientFederationClient(
      final RestClient client,
      final MeterRegistry registry
  ) {
    this.client = client;
    this.registry = registry;
  }

  @Override
  public EntityStatement entityConfiguration(final FederationRequest<EntityConfigurationRequest> request) {
    final String jwt = Optional.ofNullable(request.parameters().ecLocation())
        .map(location -> {
          if (location.startsWith("data:application/entity-statement+jwt,")) {
            return location.split(",")[1];
          }
          return this.client.mutate()
              .baseUrl(location)
              .build()
              .get()
              .retrieve()
              .body(String.class);
        })
        .orElseGet(
            () -> this.client.mutate()
                .baseUrl(request.parameters().entityID().getValue())
                .build()
                .get()
                .uri(builder -> builder.path("/.well-known/openid-federation").build()).retrieve()
                .body(String.class)
        );
    try {
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", request.parameters().entityID().getValue()),
          Tag.of("outcome", "success")
      )).increment();
      return EntityStatement.parse(jwt);
    } catch (final ParseException e) {
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", request.parameters().entityID().getValue()),
          Tag.of("outcome", "failure")
      )).increment();
      throw new RuntimeException(e);
    } catch (final Exception e) {
      this.registry.counter("GET_entity_configuration", List.of(
          Tag.of("entityId", request.parameters().entityID().getValue()),
          Tag.of("outcome", "failure")
      )).increment();
      throw e;
    }
  }

  @Override
  public EntityStatement fetch(final FederationRequest<FetchRequest> request) {
    final String url = Optional.ofNullable(request.federationEntityMetadata().get("federation_fetch_endpoint"))
        .filter(u -> u instanceof String)
        .map(String.class::cast)
        .orElseThrow();
    final String body = this.client.mutate().baseUrl(url).build()
        .get()
        .uri(builder -> builder
            .queryParam("sub", request.parameters()
                .subject())
            .build())
        .retrieve()
        .body(String.class);
    try {
      return EntityStatement.parse(body);
    } catch (final ParseException e) {
      throw new RegistryResponseException("Failed to fetch entity statement", e);
    }
  }

  @Override
  public List<String> subordinateListing(final FederationRequest<SubordinateListingRequest> request) {
    final String url = Optional.ofNullable(request.federationEntityMetadata().get("federation_list_endpoint"))
        .filter(u -> u instanceof String)
        .map(String.class::cast)
        .orElseThrow();

    return (List<String>) this.client.mutate().baseUrl(url).build()
        .get()
        .retrieve()
        .body(List.class);
  }

  @Override
  public SignedJWT trustMark(final FederationRequest<TrustMarkRequest> request) {
    final String path = Optional.ofNullable(request.federationEntityMetadata().get("federation_trust_mark_endpoint"))
        .filter(p -> p instanceof String)
        .map(String.class::cast)
        .orElseGet(() -> request.parameters().trustMarkIssuer().getValue() + "/trust_mark");
    final String body = this.client.mutate().baseUrl(path).build()
        .get()
        .uri(buidler -> buidler
            .queryParam("trust_mark_id", request.parameters().trustMarkId().getValue())
            .queryParam("sub", request.parameters().subject().getValue())
            .build())
        .retrieve()
        .body(String.class);
    try {
      return SignedJWT.parse(body);
    } catch (final java.text.ParseException e) {
      throw new RegistryResponseException("Failed to fetch entity statement", e);
    }
  }

  @Override
  public SignedJWT resolve(final FederationRequest<ResolveRequest> request) {
    return null;
  }

  @Override
  public List<String> trustMarkedListing(final FederationRequest<TrustMarkListingRequest> request) {
    return List.of();
  }
}
