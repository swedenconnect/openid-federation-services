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
package se.digg.oidfed.service.resolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import se.digg.oidfed.resolver.integration.EntityStatementIntegration;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Spring {@link RestClient} implementatoin of {@link EntityStatementIntegration}
 *
 * @author Felix Hellman
 */
public class RestClientEntityStatementIntegration implements EntityStatementIntegration {

  private final RestClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor.
   * @param client to use
   */
  public RestClientEntityStatementIntegration(final RestClient client) {
    this.client = client;
  }

  @Override
  public EntityStatement getEntityStatement(final String location) {
    try {
      final ResponseEntity<String> entity = this.client.get()
          .uri(URI.create(location)).retrieve().toEntity(String.class);
      final String body = entity.getBody();
      Objects.requireNonNull(body);
      return EntityStatement.parse(body);
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<String> getSubordinateListing(final String location) {
    try {
      final String json = this.client.get()
          .uri(location).retrieve().toEntity(String.class).getBody();
      final List<String> list = (List<String>) this.mapper.readValue(json, List.class);
      return list;
    }
    catch (final JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse json list", e);
    }
  }
  }
