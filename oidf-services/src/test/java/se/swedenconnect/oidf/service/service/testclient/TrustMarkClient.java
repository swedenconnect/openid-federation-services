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
package se.swedenconnect.oidf.service.service.testclient;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

public class TrustMarkClient {
  private final RestClient client;

  public TrustMarkClient(final RestClient client) {
    this.client = client;
  }

  public SignedJWT trustMark(
      @Nonnull final EntityID trustMarkIssuer,
      @Nonnull final EntityID trustMarkType,
      @Nonnull final EntityID subject) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark?");
      builder.append("sub=%s".formatted(subject.getValue()));
      builder.append("&trust_mark_type=%s".formatted(trustMarkType.getValue()));
      final String body = this.client.get()
          .uri(trustMarkIssuer.getValue() + builder)
          .retrieve()
          .body(String.class);
      return SignedJWT.parse(body);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get trust mark for trust mark issuer %s"
          .formatted(trustMarkIssuer.getValue()), e);
    }
  }

  public List<String> trustMarkListing(
      @Nonnull final EntityID trustMarkIssuer,
      @Nonnull final EntityID trustMarkType,
      @Nullable final EntityID subject) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark_listing?");
      builder.append("trust_mark_type=%s".formatted(trustMarkType.getValue()));
      Optional.ofNullable(subject).ifPresent(sub -> builder.append("&sub=%s".formatted(sub.getValue())));
      final List<String> body = (List<String>) client.get()
          .uri(trustMarkIssuer.getValue() + builder)
          .retrieve()
          .body(List.class);
      return body;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get trust mark listing for trust mark issuer %s"
          .formatted(trustMarkIssuer.getValue()), e);
    }
  }

  public String trustMarkStatus(
      final EntityID trustMarkIssuer,
      final String trustMark) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark_status?");
      builder.append("trust_mark=%s".formatted(trustMark));
      return client.get()
          .uri(trustMarkIssuer.getValue() + builder)
          .header("content-type", "application/json")
          .retrieve()
          .body(String.class);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get trust mark status for trust mark issuer %s"
          .formatted(trustMarkIssuer.getValue()), e);
    }
  }
}
