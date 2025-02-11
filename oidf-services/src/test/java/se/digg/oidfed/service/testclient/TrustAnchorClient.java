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
package se.digg.oidfed.service.testclient;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.integration.federation.SubordinateListingRequest;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public class TrustAnchorClient {
  private final RestClient client;
  private final EntityID trustAnchor;

  public TrustAnchorClient(final RestClient client, final EntityID trustAnchor) {
    this.client = client;
    this.trustAnchor = trustAnchor;
  }



  public List<String> subordinateListing(final SubordinateListingRequest request) {
    return client.mutate().baseUrl(trustAnchor.getValue()).build().get()
        .uri(uriBuilder -> uriBuilder.path("/subordinate_listing")
            .queryParamIfPresent("trust_marked", Optional.ofNullable(request.trustMarked()))
            .queryParamIfPresent("trust_mark_id", Optional.ofNullable(request.trustMarkId()))
            .queryParamIfPresent("intermediate", Optional.ofNullable(request.intermediate()))
            .queryParamIfPresent("entity_type", Optional.ofNullable(request.entityType()))
            .build()
        )
        .retrieve()
        .toEntity(List.class)
        .getBody();
  }

  public SignedJWT fetch(final EntityID subject) {
    try {
    final StringBuilder builder = new StringBuilder();
      Optional.ofNullable(subject).ifPresent(sub -> builder.append("?sub=%s".formatted(subject)));
    final String body = client.get()
        .uri(trustAnchor.getValue() + "/fetch" + builder)
        .retrieve()
        .toEntity(String.class)
        .getBody();
    return SignedJWT.parse(body);
    } catch (final ParseException e) {
      throw new RuntimeException("Failed to parse signed jwt from fetch", e);
    }
  }
}
