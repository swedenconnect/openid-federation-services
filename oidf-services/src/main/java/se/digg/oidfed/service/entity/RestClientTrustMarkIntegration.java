/*
 * Copyright 2024 Sweden Connect
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
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.integration.TrustMarkIntegration;
import se.digg.oidfed.common.entity.integration.TrustMarkRequest;

import java.text.ParseException;
import java.util.Map;

/**
 * {@link RestClient} implementation of {@link TrustMarkIntegration}
 *
 * @author Felix Hellman
 */
public class RestClientTrustMarkIntegration implements TrustMarkIntegration {
  private final RestClient client;

  /**
   * Constructor.
   * @param client for rest
   */
  public RestClientTrustMarkIntegration(final RestClient client) {
    this.client = client;
  }

  @Override
  public SignedJWT getTrustMark(final TrustMarkRequest request) {
    try {
      final String body = this.client.get()
          .uri(request.trustMarkIssuer().getValue() + "/trust_mark?trust_mark_id={trust_mark_id}&sub={sub}", Map.of(
              "trust_mark_id", request.trustMarkId().getValue(), "sub", request.subject().getValue()))
          .retrieve()
          .body(String.class);
      return SignedJWT.parse(body);
    } catch (final ParseException e) {
      throw new IllegalArgumentException("Response was not a signed jwt", e);
    }
  }
}
