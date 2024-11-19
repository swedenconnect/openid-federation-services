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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.common.entity.EntityRecordIntegration;

import java.text.ParseException;
import java.util.Map;

/**
 * {@link RestClient} implementation of {@link EntityRecordIntegration}
 *
 * @author Felix Hellman
 */
public class RestClientEntityRecordIntegration implements EntityRecordIntegration {
  private final RestClient client;

  /**
   * Constructor.
   * @param client to use.
   */
  public RestClientEntityRecordIntegration(final RestClient client) {
    this.client = client;
  }

  @Override
  public SignedJWT getAll(final EntityID issuer) {
    final String body = client
        .get()
        .uri("/registry/v1/entities", Map.of("iss", issuer.getValue()))
        .retrieve()
        .body(String.class);
    try {
      return SignedJWT.parse(body);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
