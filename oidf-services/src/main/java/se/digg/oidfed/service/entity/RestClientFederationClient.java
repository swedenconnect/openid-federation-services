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
package se.digg.oidfed.service.entity;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.integration.registry.RegistryResponseException;
import se.digg.oidfed.common.entity.integration.federation.EntityConfigurationRequest;
import se.digg.oidfed.common.entity.integration.federation.FederationClient;
import se.digg.oidfed.common.entity.integration.federation.FederationRequest;
import se.digg.oidfed.common.entity.integration.federation.FetchRequest;
import se.digg.oidfed.common.entity.integration.federation.ResolveRequest;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkListingRequest;
import se.digg.oidfed.common.entity.integration.federation.TrustMarkRequest;

import java.util.List;
import java.util.Optional;

/**
 * {@link RestClient} implementation of {@link FederationClient}.
 *
 * @author Felix Hellman
 */
public class RestClientFederationClient implements FederationClient {
  private final RestClient client;

  /**
   * @param client to use for requests
   */
  public RestClientFederationClient(final RestClient client) {
    this.client = client;
  }

  @Override
  public EntityStatement entityConfiguration(final FederationRequest<EntityConfigurationRequest> request) {
    final String jwt = Optional.ofNullable(request.federationEntityMetadata()
            .get("subject_entity_configuration_location"))
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
      return EntityStatement.parse(jwt);
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public EntityStatement fetch(final FederationRequest<FetchRequest> request) {
    final String url = request.federationEntityMetadata().get("federation_fetch_endpoint");
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
    final String url = request.federationEntityMetadata().get("federation_list_endpoint");
    return (List<String>) this.client.mutate().baseUrl(url).build()
        .get()
        .retrieve()
        .body(List.class);
  }

  @Override
  public SignedJWT trustMark(final FederationRequest<TrustMarkRequest> request) {
    final String path = Optional.ofNullable(request.federationEntityMetadata().get("federation_trust_mark_endpoint"))
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
