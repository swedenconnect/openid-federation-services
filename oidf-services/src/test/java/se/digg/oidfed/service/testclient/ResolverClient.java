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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import org.springframework.web.client.RestClient;

import java.util.Optional;

public class ResolverClient {
  private final RestClient client;
  private final EntityID resolver;

  public ResolverClient(final RestClient client, final EntityID resolver) {
    this.client = client;
    this.resolver = resolver;
  }

  public SignedJWT resolve(final EntityID subject, final EntityID anchor, final EntityType type) {
    try {
      final StringBuilder builder = new StringBuilder("/resolve?");
      builder.append("sub=%s".formatted(subject.getValue()));
      builder.append("&trust_anchor=%s".formatted(anchor.getValue()));
      Optional.ofNullable(type).ifPresent(t -> builder.append("&type=%s".formatted(t.getValue())));
      final String body = client.get()
          .uri(resolver.getValue() + builder)
          .retrieve()
          .body(String.class);
      return SignedJWT.parse(body);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to resolve for resolver %s".formatted(resolver.getValue()));
    }
  }
}
