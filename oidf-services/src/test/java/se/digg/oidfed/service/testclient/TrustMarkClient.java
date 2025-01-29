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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.service.trustmarkissuer.TrustMarkIssuerController;

import java.util.List;
import java.util.Optional;

public class TrustMarkClient {
  private final RestClient client;

  public TrustMarkClient(final RestClient client) {
    this.client = client;
  }

  public SignedJWT trustMark(
      @Nonnull final EntityID trustMarkIssuer,
      @Nonnull final EntityID trustMarkId,
      @Nonnull final EntityID subject) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark?");
      builder.append("sub=%s".formatted(subject.getValue()));
      builder.append("&trust_mark_id=%s".formatted(trustMarkId.getValue()));
      final String body = client.get()
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
      @Nonnull final EntityID trustMarkId,
      @Nullable final EntityID subject) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark_listing?");
      builder.append("trust_mark_id=%s".formatted(trustMarkId.getValue()));
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

  public TrustMarkIssuerController.TrustMarkStatusReply trustMarkStatus(
      final EntityID trustMarkIssuer,
      final EntityID trustMarkId,
      final EntityID subject,
      final Long issueTime) {
    try {
      final StringBuilder builder = new StringBuilder("/trust_mark_status?");
      builder.append("trust_mark_id=%s".formatted(trustMarkId.getValue()));
      Optional.ofNullable(subject).ifPresent(sub -> builder.append("&sub=%s".formatted(sub.getValue())));
      Optional.ofNullable(issueTime).ifPresent(iat -> builder.append("&iat=%d".formatted(iat)));
      return client.post()
          .uri(trustMarkIssuer.getValue() + builder)
          .retrieve()
          .body(TrustMarkIssuerController.TrustMarkStatusReply.class);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get trust mark status for trust mark issuer %s"
          .formatted(trustMarkIssuer.getValue()), e);
    }
  }
}
